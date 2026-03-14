# Feature Specification: Lead Qualification Workflow

## 1. Feature Metadata

**Feature Name:** Lead Qualification Workflow  
**Feature ID:** CRM-016  
**Version:** v1.0  
**Owner Team:** Fullstack  
**Priority:** CRITICAL  
**Status:** APPROVED

**Description:**
Implement a structured lead qualification workflow that guides sales representatives through qualifying leads from initial contact (NEW) to qualified status (QUALIFIED), with automatic scoring, required qualification criteria validation, and optional conversion to customer + opportunity. The workflow enforces business rules, tracks qualification history, and triggers notifications at key milestones.

**Business Goal:**
Increase lead-to-customer conversion rate from 12% to 18% by ensuring consistent qualification criteria are applied across all leads. Reduce time-to-qualification from average 7 days to 3 days by providing clear qualification checklists and automated scoring.

---

## 2. Actors

| Actor | Role Code | Description |
|---|---|---|
| Sales Representative | SALES_REP | Qualifies own leads, updates qualification criteria, views lead score |
| Sales Manager | SALES_MANAGER | Reviews team lead qualifications, overrides rejection, approves conversions |
| CRM Administrator | CRM_ADMIN | Configures qualification criteria weights, views all lead workflows |
| System | SYSTEM | Calculates lead scores automatically, sends qualification reminders |

**RBAC Traceability:**
- Permissions: `lead:read`, `lead:update`, `lead:convert` (from `docs/architecture/rbac-model.md`)
- Data Scope: SALES_REP (OWN), SALES_MANAGER (TEAM), CRM_ADMIN (ALL)

---

## 3. Business Context

**When:**
- Sales Rep receives new lead from marketing campaign, trade show, or website form
- Lead has expressed interest but needs vetting before sales resources allocated
- Manager needs visibility into team qualification progress

**Why:**
- Unqualified leads waste sales time (average 45 minutes per unqualified lead)
- Inconsistent qualification leads to poor-fit customers churning within 6 months
- Manual scoring is subjective and biased

**System Boundaries:**
- **In Scope:** crm-service (lead qualification state machine, scoring), notification-service (milestone alerts), analytics-service (qualification metrics)
- **Out of Scope:** AI-powered auto-qualification (v2 feature), third-party data enrichment (Clearbit, ZoomInfo) integration (v2), mobile app qualification interface (v2)

---

## 4. Business Flow

**Step 1:** Sales Rep views NEW lead on lead detail page `/crm/leads/{id}`  
**Step 2:** System displays "Qualification Checklist" panel with 5 criteria (budget, authority, need, timeline, fit)  
**Step 3:** Rep clicks "Start Qualification" → Lead status transitions NEW → QUALIFYING  
**Step 4:** Rep fills qualification criteria form:
- Budget: < $10K (10 pts) | $10K-$50K (20 pts) | > $50K (30 pts)
- Authority: Influencer (10 pts) | Decision Maker (25 pts)
- Need: Nice-to-have (5 pts) | Critical pain (20 pts)
- Timeline: > 6 months (5 pts) | 1-6 months (15 pts) | < 1 month (25 pts)
- Fit: Poor (0 pts) | Fair (10 pts) | Excellent (20 pts)  
**Step 5:** System calculates lead_score (sum of points, max 120)  
**Step 6:** Rep adds qualification notes (required, min 50 chars)  
**Step 7:** Rep clicks "Submit Qualification"  
**Step 8:** System validates: lead_score ≥ 60 required for QUALIFIED status  
**Step 9a:** If score ≥ 60 → Status transitions QUALIFYING → QUALIFIED, send notification to Manager  
**Step 9b:** If score < 60 → Status transitions QUALIFYING → DISQUALIFIED, send notification to Rep with reason  
**Step 10:** (If QUALIFIED) Rep can now click "Convert to Customer" → Opens conversion modal  
**Step 11:** Rep fills conversion form: opportunity_name, expected_value, expected_close_date  
**Step 12:** System creates Customer entity (from lead contact info) + Opportunity entity (NEW stage)  
**Step 13:** System transitions Lead status QUALIFIED → CONVERTED  
**Step 14:** System sends notification to Manager: "Lead {name} converted by {rep_name}, opportunity value: ${value}"  

---

## 5. API Contract

### 5.1 Start Lead Qualification

**Endpoint:** `POST /api/crm/leads/{leadId}/qualification/start`  
**Method:** POST  
**Description:** Transition lead from NEW to QUALIFYING status

**Request:** (No body required)

**Response:**
```json
{
  "lead_id": "770e8400-e29b-41d4-a716-446655440000",
  "status": "QUALIFYING",
  "qualification_started_at": "2026-03-13T14:00:00Z",
  "qualification_started_by": "user-uuid-rep1"
}
```

**Status Codes:**
- 200: Qualification started
- 404: Lead not found (CRM_LEAD_NOT_FOUND)
- 409: Lead already in QUALIFYING or later state (CRM_LEAD_INVALID_STATE_TRANSITION)
- 403: User lacks permission or data-scope access (FORBIDDEN)

---

### 5.2 Submit Lead Qualification

**Endpoint:** `POST /api/crm/leads/{leadId}/qualification/submit`  
**Method:** POST  
**Description:** Submit qualification criteria and calculate score

**Request:**
```json
{
  "budget_range": "HIGH",
  "authority_level": "DECISION_MAKER",
  "need_urgency": "CRITICAL",
  "timeline": "SHORT",
  "fit_score": "EXCELLENT",
  "qualification_notes": "Decision maker confirmed $80K budget for Q2 implementation. Pain point: manual processes causing 20% revenue loss. Excellent product-market fit."
}
```

**Fields:**
- `budget_range` (enum, required): "LOW" | "MEDIUM" | "HIGH"
- `authority_level` (enum, required): "INFLUENCER" | "DECISION_MAKER"
- `need_urgency` (enum, required): "NICE_TO_HAVE" | "CRITICAL"
- `timeline` (enum, required): "LONG" | "MEDIUM" | "SHORT"
- `fit_score` (enum, required): "POOR" | "FAIR" | "EXCELLENT"
- `qualification_notes` (string, required): Min 50 chars, max 1000 chars

**Response (Qualified):**
```json
{
  "lead_id": "770e8400-e29b-41d4-a716-446655440000",
  "status": "QUALIFIED",
  "lead_score": 95,
  "score_breakdown": {
    "budget": 30,
    "authority": 25,
    "need": 20,
    "timeline": 15,
    "fit": 20
  },
  "qualified_at": "2026-03-13T14:05:00Z",
  "qualified_by": "user-uuid-rep1"
}
```

**Response (Disqualified):**
```json
{
  "lead_id": "770e8400-e29b-41d4-a716-446655440000",
  "status": "DISQUALIFIED",
  "lead_score": 45,
  "score_breakdown": {
    "budget": 10,
    "authority": 10,
    "need": 5,
    "timeline": 15,
    "fit": 10
  },
  "disqualification_reason": "Lead score 45 below minimum threshold of 60. Low budget and poor product fit.",
  "disqualified_at": "2026-03-13T14:05:00Z"
}
```

**Status Codes:**
- 200: Qualification submitted, status updated
- 400: Invalid enum values or notes too short (CRM_LEAD_QUALIFICATION_INVALID)
- 404: Lead not found
- 409: Lead not in QUALIFYING status (CRM_LEAD_INVALID_STATE_TRANSITION)

---

### 5.3 Convert Lead to Customer + Opportunity

**Endpoint:** `POST /api/crm/leads/{leadId}/convert`  
**Method:** POST  
**Description:** Convert QUALIFIED lead to Customer entity and create Opportunity

**Request:**
```json
{
  "opportunity_name": "ABC Corp - Enterprise License Q2",
  "expected_value": 80000.00,
  "expected_close_date": "2026-06-30",
  "opportunity_notes": "Follow-up on qualification call. Decision maker ready to proceed with POC."
}
```

**Fields:**
- `opportunity_name` (string, required): Max 200 chars
- `expected_value` (decimal, required): > 0, max 2 decimal places
- `expected_close_date` (date, required): Must be future date
- `opportunity_notes` (string, optional): Max 1000 chars

**Response:**
```json
{
  "lead_id": "770e8400-e29b-41d4-a716-446655440000",
  "lead_status": "CONVERTED",
  "customer_id": "880e8400-e29b-41d4-a716-446655440001",
  "customer_code": "CUST-ABC-001",
  "opportunity_id": "990e8400-e29b-41d4-a716-446655440002",
  "opportunity_stage": "NEW",
  "converted_at": "2026-03-13T14:10:00Z",
  "converted_by": "user-uuid-rep1"
}
```

**Status Codes:**
- 201: Conversion successful, customer + opportunity created
- 400: Invalid input (CRM_LEAD_CONVERSION_INVALID_INPUT)
- 404: Lead not found
- 409: Lead not in QUALIFIED status (CRM_LEAD_NOT_QUALIFIED)
- 409: Lead already converted (CRM_LEAD_ALREADY_CONVERTED)

---

### 5.4 Get Lead Qualification History

**Endpoint:** `GET /api/crm/leads/{leadId}/qualification/history`  
**Method:** GET  
**Description:** Retrieve qualification attempts and score history

**Response:**
```json
{
  "lead_id": "770e8400-e29b-41d4-a716-446655440000",
  "current_status": "QUALIFIED",
  "current_score": 95,
  "qualification_attempts": [
    {
      "attempt_id": "hist-001",
      "attempted_at": "2026-03-10T10:00:00Z",
      "attempted_by": "user-uuid-rep1",
      "score": 52,
      "outcome": "DISQUALIFIED",
      "reason": "Score below threshold"
    },
    {
      "attempt_id": "hist-002",
      "attempted_at": "2026-03-13T14:05:00Z",
      "attempted_by": "user-uuid-rep1",
      "score": 95,
      "outcome": "QUALIFIED",
      "reason": null
    }
  ]
}
```

**Status Codes:**
- 200: History retrieved
- 404: Lead not found

---

## 6. Validation Rules

| Field | Rule | Error Code |
|---|---|---|
| budget_range | Required, must be: LOW, MEDIUM, HIGH | CRM_LEAD_BUDGET_REQUIRED, CRM_LEAD_BUDGET_INVALID |
| authority_level | Required, must be: INFLUENCER, DECISION_MAKER | CRM_LEAD_AUTHORITY_REQUIRED, CRM_LEAD_AUTHORITY_INVALID |
| need_urgency | Required, must be: NICE_TO_HAVE, CRITICAL | CRM_LEAD_NEED_REQUIRED, CRM_LEAD_NEED_INVALID |
| timeline | Required, must be: LONG, MEDIUM, SHORT | CRM_LEAD_TIMELINE_REQUIRED, CRM_LEAD_TIMELINE_INVALID |
| fit_score | Required, must be: POOR, FAIR, EXCELLENT | CRM_LEAD_FIT_REQUIRED, CRM_LEAD_FIT_INVALID |
| qualification_notes | Required, min 50 chars, max 1000 chars | CRM_LEAD_NOTES_REQUIRED, CRM_LEAD_NOTES_TOO_SHORT |
| opportunity_name | Required for conversion, max 200 chars | CRM_OPPORTUNITY_NAME_REQUIRED |
| expected_value | Required for conversion, > 0, decimal(10,2) | CRM_OPPORTUNITY_VALUE_REQUIRED, CRM_OPPORTUNITY_VALUE_INVALID |
| expected_close_date | Required for conversion, future date | CRM_OPPORTUNITY_DATE_REQUIRED, CRM_OPPORTUNITY_DATE_PAST |

**Business Rule:** Lead score must be ≥ 60 to achieve QUALIFIED status

---

## 7. Domain Model

### 7.1 Lead (Extended)

**Table:** `crm_leads` (existing table, add columns)

| Field | Type | Constraints | Description |
|---|---|---|---|
| lead_score | INTEGER | NULL | Calculated qualification score (0-120) |
| qualified_at | TIMESTAMP | NULL | Timestamp when status became QUALIFIED |
| qualified_by | UUID | NULL | User who qualified the lead |
| disqualified_at | TIMESTAMP | NULL | Timestamp when status became DISQUALIFIED |
| disqualification_reason | VARCHAR(500) | NULL | Reason for disqualification |
| converted_at | TIMESTAMP | NULL | Timestamp when converted to customer |
| converted_by | UUID | NULL | User who performed conversion |
| customer_id | UUID | NULL | Reference to created Customer |
| opportunity_id | UUID | NULL | Reference to created Opportunity |

**New Indexes:**
- `idx_leads_lead_score` (lead_score DESC) - for top leads query
- `idx_leads_qualified_at` (qualified_at) - for qualification metrics

---

### 7.2 LeadQualificationCriteria

**Table:** `crm_lead_qualification_criteria`

| Field | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PK | Criteria record identifier |
| lead_id | UUID | NOT NULL, FK | Reference to Lead |
| budget_range | VARCHAR(20) | NOT NULL | LOW, MEDIUM, HIGH |
| authority_level | VARCHAR(30) | NOT NULL | INFLUENCER, DECISION_MAKER |
| need_urgency | VARCHAR(30) | NOT NULL | NICE_TO_HAVE, CRITICAL |
| timeline | VARCHAR(20) | NOT NULL | LONG, MEDIUM, SHORT |
| fit_score | VARCHAR(20) | NOT NULL | POOR, FAIR, EXCELLENT |
| qualification_notes | TEXT | NOT NULL | Sales rep notes (min 50 chars) |
| calculated_score | INTEGER | NOT NULL | Sum of criteria points |
| qualified_at | TIMESTAMP | NOT NULL | Submission timestamp |
| qualified_by | UUID | NOT NULL | User who submitted |
| created_at | TIMESTAMP | NOT NULL | Audit |
| created_by | UUID | NOT NULL | Audit |

**Indexes:**
- `idx_qualification_criteria_lead_id` (lead_id) - for history lookup
- `idx_qualification_criteria_qualified_at` (qualified_at DESC) - for recent qualifications

---

### 7.3 LeadQualificationHistory

**Table:** `crm_lead_qualification_history`

| Field | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PK | History record identifier |
| lead_id | UUID | NOT NULL, FK | Reference to Lead |
| attempt_number | INTEGER | NOT NULL | 1, 2, 3... (incremental) |
| from_status | VARCHAR(30) | NOT NULL | Previous lead status |
| to_status | VARCHAR(30) | NOT NULL | New lead status |
| score | INTEGER | NULL | Qualification score (if applicable) |
| outcome | VARCHAR(30) | NOT NULL | QUALIFIED, DISQUALIFIED |
| reason | VARCHAR(500) | NULL | Disqualification reason or notes |
| attempted_at | TIMESTAMP | NOT NULL | Attempt timestamp |
| attempted_by | UUID | NOT NULL | User who attempted |

**Indexes:**
- `idx_qualification_history_lead_id_attempt` (lead_id, attempt_number DESC)

---

## 8. State Machine: Lead Qualification Workflow

**States:**
- **NEW**: Initial state when lead created
- **QUALIFYING**: Rep actively filling qualification criteria
- **QUALIFIED**: Lead meets minimum score threshold (≥60), ready for conversion
- **DISQUALIFIED**: Lead does not meet criteria, removed from active pipeline
- **CONVERTED**: Lead successfully converted to Customer + Opportunity

**Allowed Transitions:**
- NEW → QUALIFYING (via "Start Qualification" action)
- QUALIFYING → QUALIFIED (score ≥ 60)
- QUALIFYING → DISQUALIFIED (score < 60)
- QUALIFIED → CONVERTING (intermediate state during conversion)
- CONVERTING → CONVERTED (conversion successful)
- DISQUALIFIED → QUALIFYING (manager override, re-qualification allowed)

**Forbidden Transitions:**
- NEW → QUALIFIED (must go through QUALIFYING)
- DISQUALIFIED → CONVERTED (must re-qualify first)
- CONVERTED → any state (terminal state)

**Permission Guards:**
- NEW → QUALIFYING: Requires `lead:update`, data-scope OWN/TEAM/ALL
- QUALIFYING → QUALIFIED/DISQUALIFIED: System calculates, user submits
- QUALIFIED → CONVERTED: Requires `lead:convert` (SALES_MANAGER or CRM_ADMIN for non-owned leads)
- DISQUALIFIED → QUALIFYING: Requires `lead:update` + SALES_MANAGER or CRM_ADMIN role

---

## 9. External Integrations

### 9.1 Notification Service

**Service:** notification-service  
**Endpoint:** `POST /api/notifications/send`  
**Purpose:** Send email/SMS notifications at qualification milestones  
**Timeout:** 5 seconds  
**Retry Policy:** 3 attempts with backoff  
**Fallback:** Log notification failure, do not block qualification flow

**Notification Events:**
- Lead qualified: Notify SALES_MANAGER
- Lead disqualified: Notify owner SALES_REP with improvement tips
- Lead converted: Notify SALES_MANAGER with opportunity details

---

### 9.2 Analytics Service

**Service:** analytics-service  
**Endpoint:** `POST /api/analytics/events`  
**Purpose:** Track qualification metrics for reporting dashboard  
**Timeout:** 3 seconds  
**Retry Policy:** 2 attempts  
**Fallback:** Log failure, continue (analytics non-critical)

**Events Tracked:**
- lead.qualification.started
- lead.qualification.completed (with outcome: QUALIFIED or DISQUALIFIED)
- lead.converted

---

## 10. Event Flow

### Event: lead.qualification.started

**Payload:**
```json
{
  "event_type": "lead.qualification.started",
  "lead_id": "770e8400-e29b-41d4-a716-446655440000",
  "lead_name": "ABC Corporation",
  "owner_user_id": "user-uuid-rep1",
  "timestamp": "2026-03-13T14:00:00Z",
  "trace_id": "trace-abc-123"
}
```

**Subscribers:** analytics-service (start timer for time-to-qualify metric)

---

### Event: lead.qualification.completed

**Payload:**
```json
{
  "event_type": "lead.qualification.completed",
  "lead_id": "770e8400-e29b-41d4-a716-446655440000",
  "outcome": "QUALIFIED",
  "score": 95,
  "qualified_by": "user-uuid-rep1",
  "timestamp": "2026-03-13T14:05:00Z",
  "trace_id": "trace-abc-123"
}
```

**Subscribers:** notification-service (send alert), analytics-service (record metric)

---

### Event: lead.converted

**Payload:**
```json
{
  "event_type": "lead.converted",
  "lead_id": "770e8400-e29b-41d4-a716-446655440000",
  "customer_id": "880e8400-e29b-41d4-a716-446655440001",
  "opportunity_id": "990e8400-e29b-41d4-a716-446655440002",
  "expected_value": 80000.00,
  "converted_by": "user-uuid-rep1",
  "timestamp": "2026-03-13T14:10:00Z",
  "trace_id": "trace-abc-123"
}
```

**Subscribers:** notification-service (manager alert), analytics-service (conversion metric)

---

## 11. Error Codes

| Error Code | HTTP Status | Message (English) | When |
|---|---|---|---|
| CRM_LEAD_NOT_FOUND | 404 | Lead not found. | lead_id doesn't exist |
| CRM_LEAD_INVALID_STATE_TRANSITION | 409 | Invalid lead status transition from {from} to {to}. | Forbidden state transition attempted |
| CRM_LEAD_QUALIFICATION_INVALID | 400 | Qualification criteria invalid. | Missing or invalid enum values |
| CRM_LEAD_NOTES_TOO_SHORT | 400 | Qualification notes must be at least 50 characters. | notes.length < 50 |
| CRM_LEAD_NOT_QUALIFIED | 409 | Lead must be QUALIFIED status to convert. | Attempt convert non-QUALIFIED lead |
| CRM_LEAD_ALREADY_CONVERTED | 409 | Lead has already been converted. | Duplicate conversion attempt |
| CRM_OPPORTUNITY_NAME_REQUIRED | 400 | Opportunity name is required. | Empty opportunity_name |
| CRM_OPPORTUNITY_VALUE_INVALID | 400 | Expected value must be greater than 0. | value ≤ 0 |
| CRM_OPPORTUNITY_DATE_PAST | 400 | Expected close date must be in the future. | date < today |

---

## 12. Observability

### Logs

**Log Events:**
- `lead_qualification_started` (INFO)
- `lead_qualification_criteria_calculated` (DEBUG): score breakdown
- `lead_qualified` (INFO): lead_id, score, qualified_by
- `lead_disqualified` (INFO): lead_id, score, reason
- `lead_conversion_started` (INFO)
- `lead_conversion_completed` (INFO): customer_id, opportunity_id, value
- `lead_conversion_failed` (ERROR): error details

**Example Log:**
```json
{
  "timestamp": "2026-03-13T14:05:00Z",
  "level": "INFO",
  "service": "crm-service",
  "logger": "LeadQualificationService",
  "message": "lead_qualified",
  "trace_id": "trace-abc-123",
  "lead_id": "770e8400-e29b-41d4-a716-446655440000",
  "score": 95,
  "qualified_by": "user-uuid-rep1"
}
```

---

### Metrics

- `lead_qualification_attempts_total` (Counter): Total qualification submissions
- `lead_qualified_total` (Counter): Successful qualifications
- `lead_disqualified_total` (Counter): Disqualifications
- `lead_qualification_score_distribution` (Histogram): Score value distribution
- `lead_time_to_qualify_seconds` (Histogram): NEW → QUALIFIED duration
- `lead_conversion_rate` (Gauge): converted / qualified ratio
- `lead_conversion_value_total` (Counter): Sum of opportunity expected_value

---

### Tracing

**Spans:**
- `startLeadQualification`: Status transition (50ms)
- `submitLeadQualification`: Calculation + state transition (200ms)
  - Child: `calculateLeadScore` (10ms)
  - Child: `updateLeadStatus` (100ms)
  - Child: `createQualificationHistory` (50ms)
- `convertLead`: Customer + Opportunity creation (500ms)
  - Child: `createCustomerFromLead` (200ms)
  - Child: `createOpportunity` (150ms)
  - Child: `updateLeadToConverted` (100ms)

---

## 13. Security Requirements

### Authentication
- All endpoints require valid JWT token

### Authorization (RBAC)

| Endpoint | Required Permission | Data Scope |
|---|---|---|
| POST /leads/{id}/qualification/start | lead:update | OWN (SALES_REP), TEAM (SALES_MANAGER), ALL (CRM_ADMIN) |
| POST /leads/{id}/qualification/submit | lead:update | OWN/TEAM/ALL |
| POST /leads/{id}/convert | lead:convert | TEAM (SALES_MANAGER), ALL (CRM_ADMIN) |
| GET /leads/{id}/qualification/history | lead:read | OWN/TEAM/ALL |

**Special Rules:**
- SALES_REP can only convert own leads if `lead:convert` permission granted (future enhancement)
- Manager approval required for conversions > $100K (future enhancement)

### Input Validation
- Enum values whitelisted (prevent injection)
- Qualification notes sanitized (XSS prevention)
- Opportunity value capped at $10M (prevent unrealistic forecasts)

---

## 14. Performance Requirements

### Response Time (p95)
- Start qualification: < 100ms
- Submit qualification: < 300ms (includes score calculation)
- Convert lead: < 800ms (creates 2 entities + updates lead)
- Get history: < 200ms

### Throughput
- Support 100 concurrent qualifications
- Support 50 conversions per minute (peak time)

### Resource Limits
- Qualification notes: Max 1000 chars
- History retained indefinitely (no auto-purge)

---

## 15. Frontend UI Specification

### Page: Lead Detail with Qualification Panel

**Route:** `/crm/leads/{id}`  
**Access:** SALES_REP (own leads), SALES_MANAGER (team leads), CRM_ADMIN (all leads)

---

### Component: QualificationChecklist

**File:** `app/crm/leads/[id]/components/qualification-checklist.tsx`

**Display (NEW status):**
- Qualification Status Badge: "Not Qualified" (gray)
- "Start Qualification" button (primary)

**Display (QUALIFYING status):**
- Qualification Criteria Form:
  - Budget Range: Radio buttons (Low $<10K [10pts], Medium $10-50K [20pts], High $>50K [30pts])
  - Authority Level: Radio buttons (Influencer [10pts], Decision Maker [25pts])
  - Need Urgency: Radio buttons (Nice-to-have [5pts], Critical [20pts])
  - Timeline: Radio buttons (>6mo [5pts], 1-6mo [15pts], <1mo [25pts])
  - Fit Score: Radio buttons (Poor [0pts], Fair [10pts], Excellent [20pts])
  - Qualification Notes: Textarea (min 50 chars, show char count)
- Real-time Score Display: "Current Score: 95 / 120" (updates as criteria selected)
- "Submit Qualification" button (primary, disabled if notes < 50 chars)

**Display (QUALIFIED status):**
- Qualification Status Badge: "Qualified" (green, checkmark icon)
- Lead Score: "95 / 120" (large, prominent)
- Score Breakdown: Bar chart showing 5 criteria scores
- Qualified At: "March 13, 2026 at 2:05 PM by John Doe"
- "Convert to Customer" button (primary)
- "View Qualification History" link

**Display (DISQUALIFIED status):**
- Qualification Status Badge: "Disqualified" (red)
- Lead Score: "45 / 120"
- Disqualification Reason: "Lead score 45 below minimum threshold of 60. Low budget and poor product fit."
- "Re-qualify Lead" button (secondary, SALES_MANAGER only)

---

### Component: LeadConversionModal

**File:** `app/crm/leads/[id]/components/lead-conversion-modal.tsx`

**Trigger:** Click "Convert to Customer" button (QUALIFIED leads only)

**Form Fields:**
- Opportunity Name: Text input (required, max 200 chars, placeholder: "ABC Corp - Enterprise License Q2")
- Expected Value: Number input (required, $ prefix, 2 decimals, placeholder: "80,000.00")
- Expected Close Date: Date picker (required, future dates only)
- Opportunity Notes: Textarea (optional, max 1000 chars)

**Actions:**
- "Convert Lead" button (primary)
- "Cancel" button (secondary)

**States:**
- Idle: Form enabled
- Converting: Show spinner, disable form, text "Creating customer and opportunity..."
- Success: Show success message "Lead converted! Customer: CUST-ABC-001, Opportunity created.", auto-close after 3s
- Error: Show error message from API, keep modal open

---

### Component: QualificationHistoryPanel

**File:** `app/crm/leads/[id]/components/qualification-history-panel.tsx`

**Display:**
- Timeline view of qualification attempts (most recent first)
- Each attempt shows:
  - Attempt #: "Attempt 2"
  - Date/Time: "March 13, 2026 at 2:05 PM"
  - By User: "John Doe"
  - Score: "95 / 120"
  - Outcome: Badge (QUALIFIED green, DISQUALIFIED red)
  - Expandable details: Score breakdown, notes

---

## 16. Test Scenarios

### Unit Tests

#### Backend: LeadQualificationService

- `shouldCalculateScoreCorrectly()`: All HIGH criteria → score 120
- `shouldQualifyLeadWhenScoreAboveThreshold()`: Score 95 → status QUALIFIED
- `shouldDisqualifyLeadWhenScoreBelowThreshold()`: Score 45 → status DISQUALIFIED
- `shouldPreventQualificationFromInvalidStatus()`: Lead status CONVERTED → throw CRM_LEAD_INVALID_STATE_TRANSITION
- `shouldCreateQualificationHistory()`: Each attempt logged
- `shouldConvertLeadSuccessfully()`: QUALIFIED → create Customer + Opportunity → status CONVERTED
- `shouldPreventConversionOfNonQualifiedLead()`: NEW status → throw CRM_LEAD_NOT_QUALIFIED

---

#### Frontend: QualificationChecklist

- `shouldCalculateScoreLive()`: Select criteria → verify score updates
- `shouldDisableSubmitWhenNotesTooShort()`: Notes 30 chars → button disabled
- `shouldSubmitQualificationSuccessfully()`: Valid form → POST /qualification/submit → show success
- `shouldShowDisqualificationReason()`: Score < 60 → display reason message

---

### Integration Tests

#### E2E Qualification Flow

1. Login as SALES_REP
2. Navigate to NEW lead
3. Click "Start Qualification"
4. Fill criteria form (score 95)
5. Submit → Verify status QUALIFIED
6. Click "Convert to Customer"
7. Fill conversion form
8. Submit → Verify Customer + Opportunity created

---

### Failure Scenarios

- **Database error during conversion:** Rollback transaction, lead remains QUALIFIED
- **Notification service down:** Conversion completes, log notification failure
- **Concurrent qualification attempts:** Second request returns 409 CRM_LEAD_INVALID_STATE_TRANSITION

---

## 17. AI Output Expectations

### Backend Files

#### Entities
- [ ] Update `Lead.java` (add lead_score, qualified_at, converted_at fields)
- [ ] `LeadQualificationCriteria.java`
- [ ] `LeadQualificationHistory.java`

#### Services
- [ ] `LeadQualificationService.java` (scoring logic, state transitions)
- [ ] `LeadConversionService.java` (create Customer + Opportunity)

#### Controllers
- [ ] `LeadQualificationController.java` (4 endpoints)

#### DTOs
- [ ] `StartQualificationResponse.java`
- [ ] `SubmitQualificationRequest.java`
- [ ] `QualificationResponse.java`
- [ ] `ConvertLeadRequest.java`
- [ ] `ConvertLeadResponse.java`
- [ ] `QualificationHistoryResponse.java`

#### Tests
- [ ] `LeadQualificationServiceTest.java`
- [ ] `LeadConversionServiceTest.java`

#### Database
- [ ] `V016__add_lead_qualification_tables.sql`

---

### Frontend Files

#### Components
- [ ] `qualification-checklist.tsx`
- [ ] `lead-conversion-modal.tsx`
- [ ] `qualification-history-panel.tsx`

#### Hooks
- [ ] `useStartQualification.ts`
- [ ] `useSubmitQualification.ts`
- [ ] `useConvertLead.ts`
- [ ] `useQualificationHistory.ts`

#### Types
- [ ] `lead-qualification.types.ts`

#### Tests
- [ ] `qualification-checklist.test.tsx`

---

**Total Files: 20+ | AI Coverage: 93%**
