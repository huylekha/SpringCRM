# Feature Specification: Lead Qualification Workflow

## 1. Feature Metadata

**Feature Name:** Lead Qualification Workflow
**Feature ID:** CRM-016
**Version:** v1.0
**Owner Team:** Fullstack
**Priority:** HIGH
**Status:** APPROVED

**Description:**
Enable sales representatives to systematically qualify leads through a guided multi-step workflow. The feature scores leads based on BANT criteria (Budget, Authority, Need, Timeline) and automatically transitions lead status from NEW to QUALIFIED when threshold is met. Includes automated assignment of qualified leads to opportunity pipeline.

**Business Goal:**
Improve lead conversion rate from 12% to 25% by implementing structured qualification process. Reduce time-to-qualification from 7 days (ad-hoc) to 2 days (guided workflow). Enable sales managers to identify high-potential leads for priority follow-up.

---

## 2. Actors

| Actor | Role Code | Description |
|---|---|---|
| Sales Representative | SALES_REP | Qualifies own leads using BANT criteria |
| Sales Manager | SALES_MANAGER | Reviews team qualification scores, overrides if needed |
| CRM Administrator | CRM_ADMIN | Configures qualification rules and scoring weights |
| System | SYSTEM | Auto-calculates lead score, triggers state transitions |

**RBAC Trace:**
- SALES_REP: `lead:read`, `lead:update` (OWN data scope)
- SALES_MANAGER: `lead:read`, `lead:update`, `lead:convert` (TEAM data scope)
- CRM_ADMIN: `lead:read`, `lead:update`, `lead:convert` (ALL data scope)

---

## 3. Business Context

**When:**
Sales rep receives a new inbound lead (web form, cold call, referral). Rep needs to assess if lead is worth pursuing or should be marked as low-priority.

**Why:**
Unqualified leads waste 30% of sales rep time. Structured qualification ensures reps focus on high-potential leads. Historical data shows BANT-scored leads convert 3x faster than unscored leads.

**System Boundaries:**
- **In Scope:** crm-service (lead management, scoring engine), notification-service (alert on qualified leads)
- **Out of Scope:** External lead enrichment APIs (phase 2), AI-powered scoring (phase 2), automated email sequences

---

## 4. Business Flow

**Step 1:** Sales rep opens lead detail page, clicks "Start Qualification"

**Step 2:** System displays BANT criteria form with 4 sections:
- Budget: Does lead have allocated budget? (Yes/No/Unknown)
- Authority: Is lead decision-maker? (Yes/No/Influencer)
- Need: Does lead have clear business pain? (Critical/Moderate/Low)
- Timeline: Purchase timeline? (0-3 months/3-6 months/6+ months)

**Step 3:** Rep fills out criteria based on discovery conversation notes

**Step 4:** System calculates lead score (0-100):
- Budget=Yes: +30, No: +0, Unknown: +10
- Authority=Yes: +30, Influencer: +15, No: +0
- Need=Critical: +25, Moderate: +15, Low: +5
- Timeline=0-3mo: +15, 3-6mo: +10, 6+mo: +5

**Step 5:** System displays calculated score and recommendation:
- Score ≥70: "QUALIFIED - Recommended to convert to opportunity"
- Score 40-69: "NEEDS_NURTURING - Follow up in 30 days"
- Score <40: "NOT_QUALIFIED - Consider disqualifying"

**Step 6:** Rep reviews score, can override if business context justifies

**Step 7:** If score ≥70 and rep confirms, system:
- Transitions lead status: NEW → QUALIFIED
- Creates activity log entry: "Lead qualified with score X"
- Sends notification to sales manager: "New qualified lead ready for opportunity conversion"

**Step 8:** Sales manager reviews qualified leads, converts to opportunity using "Convert Lead" action

---

## 5. API Contract

### 5.1 Start Lead Qualification

**Endpoint:** `POST /api/crm/leads/{leadId}/qualification/start`
**Method:** POST
**Description:** Initialize qualification workflow for a lead

**Request:**
```json
{
  "lead_id": "uuid-lead-123"
}
```

**Response (Success):**
```json
{
  "qualification_id": "uuid-qual-456",
  "lead_id": "uuid-lead-123",
  "status": "IN_PROGRESS",
  "current_score": 0,
  "started_at": "2026-03-13T10:00:00Z",
  "started_by": "user-uuid-rep1"
}
```

**Status Codes:**
- 201: Qualification started
- 404: Lead not found (CRM_LEAD_NOT_FOUND)
- 409: Qualification already in progress (CRM_QUAL_ALREADY_EXISTS)
- 422: Lead status not eligible (must be NEW or CONTACTED) (CRM_LEAD_INVALID_STATUS)

---

### 5.2 Submit BANT Criteria

**Endpoint:** `PUT /api/crm/leads/{leadId}/qualification/bant`
**Method:** PUT
**Description:** Submit BANT criteria answers and calculate score

**Request:**
```json
{
  "budget": "YES",           // YES | NO | UNKNOWN
  "authority": "YES",         // YES | NO | INFLUENCER
  "need": "CRITICAL",         // CRITICAL | MODERATE | LOW
  "timeline": "0_3_MONTHS",   // 0_3_MONTHS | 3_6_MONTHS | 6_PLUS_MONTHS
  "notes": "CFO confirmed $50K budget allocated for Q2"
}
```

**Response:**
```json
{
  "qualification_id": "uuid-qual-456",
  "lead_id": "uuid-lead-123",
  "bant_criteria": {
    "budget": "YES",
    "authority": "YES",
    "need": "CRITICAL",
    "timeline": "0_3_MONTHS"
  },
  "score": 100,
  "recommendation": "QUALIFIED",
  "score_breakdown": {
    "budget_points": 30,
    "authority_points": 30,
    "need_points": 25,
    "timeline_points": 15
  },
  "notes": "CFO confirmed $50K budget allocated for Q2",
  "updated_at": "2026-03-13T10:05:00Z"
}
```

**Status Codes:**
- 200: BANT submitted, score calculated
- 404: Lead or qualification not found (CRM_LEAD_NOT_FOUND)
- 400: Invalid BANT values (CRM_BANT_INVALID)

---

### 5.3 Finalize Qualification

**Endpoint:** `POST /api/crm/leads/{leadId}/qualification/finalize`
**Method:** POST
**Description:** Finalize qualification, transition lead status if score ≥70

**Request:**
```json
{
  "override_score": 75,      // Optional: Manager override
  "override_reason": "Strategic account, fast-track"
}
```

**Response:**
```json
{
  "lead_id": "uuid-lead-123",
  "qualification_id": "uuid-qual-456",
  "final_score": 75,
  "recommendation": "QUALIFIED",
  "lead_status": "QUALIFIED",
  "transitioned_at": "2026-03-13T10:10:00Z",
  "notification_sent": true
}
```

**Status Codes:**
- 200: Qualification finalized, lead status updated
- 404: Lead not found (CRM_LEAD_NOT_FOUND)
- 422: Qualification not complete (missing BANT) (CRM_QUAL_INCOMPLETE)

---

### 5.4 Get Qualification History

**Endpoint:** `GET /api/crm/leads/{leadId}/qualification/history`
**Method:** GET
**Description:** Retrieve all qualification attempts for a lead

**Response:**
```json
{
  "lead_id": "uuid-lead-123",
  "qualifications": [
    {
      "qualification_id": "uuid-qual-456",
      "score": 75,
      "recommendation": "QUALIFIED",
      "started_by": "user-uuid-rep1",
      "started_at": "2026-03-13T10:00:00Z",
      "finalized_at": "2026-03-13T10:10:00Z",
      "bant_criteria": { "budget": "YES", "authority": "YES", "need": "CRITICAL", "timeline": "0_3_MONTHS" }
    }
  ]
}
```

---

## 6. Validation Rules

| Field | Rule | Error Code |
|---|---|---|
| lead_id | Required, must reference existing lead | CRM_LEAD_NOT_FOUND |
| budget | Must be one of: YES, NO, UNKNOWN | CRM_BANT_INVALID |
| authority | Must be one of: YES, NO, INFLUENCER | CRM_BANT_INVALID |
| need | Must be one of: CRITICAL, MODERATE, LOW | CRM_BANT_INVALID |
| timeline | Must be one of: 0_3_MONTHS, 3_6_MONTHS, 6_PLUS_MONTHS | CRM_BANT_INVALID |
| notes | Optional, max 1000 characters | CRM_NOTES_TOO_LONG |
| override_score | Optional, must be 0-100 if provided | CRM_SCORE_INVALID_RANGE |
| override_reason | Required if override_score provided, max 500 chars | CRM_OVERRIDE_REASON_REQUIRED |

---

## 7. Domain Model

### LeadQualification

**Table:** `crm_lead_qualifications`

| Field | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PK | Qualification record ID |
| lead_id | UUID | NOT NULL, FK(crm_leads) | Lead being qualified |
| budget | VARCHAR(20) | NOT NULL | YES, NO, UNKNOWN |
| authority | VARCHAR(20) | NOT NULL | YES, NO, INFLUENCER |
| need | VARCHAR(20) | NOT NULL | CRITICAL, MODERATE, LOW |
| timeline | VARCHAR(20) | NOT NULL | 0_3_MONTHS, 3_6_MONTHS, 6_PLUS_MONTHS |
| calculated_score | INT | NOT NULL | 0-100 |
| final_score | INT | NULL | Override score if different |
| override_reason | VARCHAR(500) | NULL | Reason for manual override |
| recommendation | VARCHAR(30) | NOT NULL | QUALIFIED, NEEDS_NURTURING, NOT_QUALIFIED |
| notes | TEXT | NULL | Free-form notes |
| status | VARCHAR(30) | NOT NULL | IN_PROGRESS, FINALIZED |
| started_by | UUID | NOT NULL | User who started qualification |
| finalized_by | UUID | NULL | User who finalized |
| started_at | TIMESTAMP | NOT NULL | Start timestamp |
| finalized_at | TIMESTAMP | NULL | Finalization timestamp |
| created_at | TIMESTAMP | NOT NULL | Audit field |
| created_by | UUID | NOT NULL | Audit field |
| updated_at | TIMESTAMP | NULL | Audit field |
| updated_by | UUID | NULL | Audit field |

**Indexes:**
- idx_lead_qual_lead_id (lead_id)
- idx_lead_qual_status (status)
- idx_lead_qual_recommendation (recommendation)
- idx_lead_qual_started_at (started_at)

---

## 8. State Machine: LeadQualification

**States:**
- IN_PROGRESS: BANT criteria being collected
- FINALIZED: Qualification complete, score locked

**Allowed Transitions:**
- IN_PROGRESS → FINALIZED (when finalize endpoint called)

**Forbidden Transitions:**
- FINALIZED → IN_PROGRESS (immutable once finalized)

**Permission Guards:**
- Only lead owner (SALES_REP), team manager (SALES_MANAGER), or CRM_ADMIN can start qualification
- Only SALES_MANAGER or CRM_ADMIN can override score

---

## 9. External Integrations

### 9.1 Notification Service

**Service:** notification-service
**Endpoint:** `POST /api/notifications/send`
**Purpose:** Send email to sales manager when lead reaches QUALIFIED status
**Timeout:** 5 seconds
**Retry Policy:** 3 attempts with exponential backoff
**Fallback:** Log failure, do not block qualification finalization

**Payload:**
```json
{
  "recipient_user_id": "manager-uuid",
  "template": "LEAD_QUALIFIED",
  "data": {
    "lead_id": "uuid-lead-123",
    "lead_name": "Acme Corp",
    "score": 75,
    "rep_name": "John Doe"
  }
}
```

---

## 10. Event Flow

### Event: lead.qualification.started

**Payload:**
```json
{
  "qualification_id": "uuid-qual-456",
  "lead_id": "uuid-lead-123",
  "started_by": "user-uuid-rep1",
  "timestamp": "2026-03-13T10:00:00Z"
}
```

**When:** Qualification workflow initiated
**Subscribers:** analytics-service (track qualification funnel metrics)

---

### Event: lead.qualified

**Payload:**
```json
{
  "lead_id": "uuid-lead-123",
  "qualification_id": "uuid-qual-456",
  "score": 75,
  "recommendation": "QUALIFIED",
  "lead_status": "QUALIFIED",
  "timestamp": "2026-03-13T10:10:00Z"
}
```

**When:** Lead transitions to QUALIFIED status
**Subscribers:** notification-service (alert manager), analytics-service (conversion metrics)

---

## 11. Error Codes

| Error Code | HTTP Status | Message (English) | When |
|---|---|---|---|
| CRM_LEAD_NOT_FOUND | 404 | Lead not found or deleted | lead_id doesn't exist |
| CRM_QUAL_ALREADY_EXISTS | 409 | Qualification already in progress for this lead | Duplicate start request |
| CRM_LEAD_INVALID_STATUS | 422 | Lead must be in NEW or CONTACTED status to qualify | Lead already QUALIFIED or CONVERTED |
| CRM_BANT_INVALID | 400 | Invalid BANT criteria value | Enum value not recognized |
| CRM_NOTES_TOO_LONG | 400 | Notes exceed 1000 character limit | notes.length > 1000 |
| CRM_SCORE_INVALID_RANGE | 400 | Override score must be between 0 and 100 | override_score < 0 or > 100 |
| CRM_OVERRIDE_REASON_REQUIRED | 400 | Override reason required when score is manually changed | override_score provided without override_reason |
| CRM_QUAL_INCOMPLETE | 422 | BANT criteria not yet submitted | Finalize called before BANT submission |

---

## 12. Observability

### Logs

**Format:** Structured JSON with trace_id

**Key Log Events:**
- `lead_qualification_started` (INFO): Qualification workflow initiated
- `lead_bant_submitted` (INFO): BANT criteria collected, score calculated
- `lead_qualification_finalized` (INFO): Qualification complete, lead status updated
- `lead_score_overridden` (WARN): Manager manually overrode score
- `lead_qualification_failed` (ERROR): Qualification process aborted due to error

**Example Log:**
```json
{
  "timestamp": "2026-03-13T10:00:00Z",
  "level": "INFO",
  "logger": "LeadQualificationService",
  "message": "lead_qualification_started",
  "trace_id": "abc123",
  "lead_id": "uuid-lead-123",
  "qualification_id": "uuid-qual-456",
  "started_by": "user-uuid-rep1"
}
```

---

### Metrics

**Custom Metrics:**
- `lead_qualification_started_count` (Counter): Total qualifications started
- `lead_qualification_score_distribution` (Histogram): Distribution of scores (0-100)
- `lead_qualification_duration_seconds` (Histogram): Time from start to finalize
- `lead_qualified_count` (Counter): Leads reaching QUALIFIED status
- `lead_qualification_conversion_rate` (Gauge): qualified / started

---

### Tracing

**Spans:**
- `startLeadQualification`: Initialize workflow
- `submitBANTCriteria`: Calculate score
- `finalizeQualification`: Update lead status, send notifications

**Span Attributes:**
- `lead_id`, `qualification_id`, `score`, `recommendation`

---

## 13. Security Requirements

### Authentication
- All endpoints require valid JWT token

### Authorization (RBAC)
| Endpoint | Required Permission | Data Scope |
|---|---|---|
| POST /leads/{id}/qualification/start | lead:update | OWN/TEAM/ALL per role |
| PUT /leads/{id}/qualification/bant | lead:update | OWN/TEAM/ALL per role |
| POST /leads/{id}/qualification/finalize | lead:update | OWN/TEAM/ALL per role |
| GET /leads/{id}/qualification/history | lead:read | OWN/TEAM/ALL per role |

**Score Override Permission:**
- Only SALES_MANAGER or CRM_ADMIN can provide `override_score`

### Input Validation
- Enum validation for BANT values (whitelist)
- Length validation for notes and override_reason
- Range validation for override_score (0-100)

---

## 14. Performance Requirements

### Response Time (p95)
- Start qualification: < 200ms
- Submit BANT: < 300ms (includes score calculation)
- Finalize qualification: < 500ms (includes lead update + notification)
- Get history: < 150ms

### Throughput
- Support 100 concurrent qualifications
- Handle 500 BANT submissions per minute

### Resource Limits
- Max 10 qualification attempts per lead (prevent spam)
- Qualification expires after 7 days if not finalized

---

## 15. Frontend UI Specification

### Page: Lead Qualification

**Route:** `/crm/leads/{leadId}/qualification`
**Access:** SALES_REP (own leads), SALES_MANAGER (team leads), CRM_ADMIN (all leads)

---

### Components

#### 1. QualificationStartButton
**Location:** Lead detail page
**Display:** "Start Qualification" button
**State:**
- Enabled: Lead status = NEW or CONTACTED
- Disabled: Lead already QUALIFIED or CONVERTED
**Action:** POST /qualification/start → Navigate to qualification form

---

#### 2. BANTCriteriaForm
**Fields:**
- Budget: Radio buttons (Yes / No / Unknown)
- Authority: Radio buttons (Yes / No / Influencer)
- Need: Radio buttons (Critical / Moderate / Low)
- Timeline: Radio buttons (0-3 months / 3-6 months / 6+ months)
- Notes: Textarea (max 1000 chars)

**Validation:**
- All BANT fields required
- Notes optional, max 1000 chars

**Interactions:**
- On field change → real-time score preview
- On Submit → PUT /qualification/bant → Show score result

---

#### 3. ScoreResultDisplay
**Display:**
- Calculated Score: 75/100 (large, prominent)
- Recommendation badge: QUALIFIED (green) / NEEDS_NURTURING (yellow) / NOT_QUALIFIED (red)
- Score breakdown table:
  - Budget: 30 points
  - Authority: 30 points
  - Need: 25 points
  - Timeline: 15 points

**Actions:**
- "Finalize Qualification" button (if score ≥70)
- "Save and Continue Later" button (all scores)
- "Override Score" button (SALES_MANAGER+ only)

---

#### 4. ScoreOverrideModal (SALES_MANAGER+)
**Fields:**
- Override Score: Number input (0-100)
- Reason: Textarea (required, max 500 chars)

**Validation:**
- Score 0-100
- Reason required

**Action:** POST /qualification/finalize with override_score

---

#### 5. QualificationHistoryTable
**Columns:**
- Date
- Score
- Recommendation
- Qualified By
- Status

**Data:** GET /qualification/history

---

### Error Handling
- CRM_LEAD_NOT_FOUND → Show toast: "Lead not found"
- CRM_QUAL_ALREADY_EXISTS → Show toast: "Qualification already in progress"
- CRM_BANT_INVALID → Show inline field error

---

## 16. Test Scenarios

### Unit Tests

#### Backend (LeadQualificationService)
- `shouldCalculateScoreCorrectly_AllYes()`: Budget=YES, Authority=YES, Need=CRITICAL, Timeline=0-3mo → Score=100
- `shouldCalculateScoreCorrectly_Mixed()`: Budget=UNKNOWN, Authority=INFLUENCER, Need=MODERATE, Timeline=3-6mo → Score=50
- `shouldRecommendQualified_ScoreAbove70()`: Score=75 → recommendation=QUALIFIED
- `shouldRecommendNeedsNurturing_Score40to69()`: Score=50 → recommendation=NEEDS_NURTURING
- `shouldRecommendNotQualified_ScoreBelow40()`: Score=30 → recommendation=NOT_QUALIFIED
- `shouldTransitionLeadStatus_WhenQualified()`: Finalize with score ≥70 → lead.status=QUALIFIED
- `shouldAllowScoreOverride_ForManager()`: SALES_MANAGER provides override_score → use override instead of calculated

#### Frontend (BANTCriteriaForm)
- `shouldCalculateScoreInRealTime()`: Change BANT fields → score preview updates
- `shouldValidateRequiredFields()`: Submit with missing BANT → show validation errors
- `shouldShowRecommendationBadge()`: Score 80 → green "QUALIFIED" badge

---

### Integration Tests

#### API Integration
- POST /qualification/start → PUT /bant → POST /finalize → verify lead.status=QUALIFIED in DB

#### Service Integration
- Mock notification-service → verify email sent after finalization

---

### Failure Scenarios

#### Error Handling
- Lead not found → 404 CRM_LEAD_NOT_FOUND
- Duplicate start request → 409 CRM_QUAL_ALREADY_EXISTS
- Invalid BANT value → 400 CRM_BANT_INVALID
- Override without reason → 400 CRM_OVERRIDE_REASON_REQUIRED

#### Edge Cases
- Start qualification for CONVERTED lead → 422 CRM_LEAD_INVALID_STATUS
- Finalize before submitting BANT → 422 CRM_QUAL_INCOMPLETE
- Notes exceed 1000 chars → 400 CRM_NOTES_TOO_LONG

---

## 17. AI Output Expectations

### Backend Generated:
- [ ] `LeadQualification.java` entity with BANT fields, score, recommendation
- [ ] `LeadQualificationRepository.java`
- [ ] `LeadQualificationService.java` with score calculation logic
- [ ] `LeadQualificationController.java` with 4 endpoints (start, bant, finalize, history)
- [ ] `StartQualificationRequest.java` DTO
- [ ] `SubmitBANTRequest.java` DTO
- [ ] `FinalizeQualificationRequest.java` DTO
- [ ] `QualificationResponse.java` DTO
- [ ] `QualificationHistoryResponse.java` DTO
- [ ] Error codes: CRM_LEAD_NOT_FOUND, CRM_QUAL_ALREADY_EXISTS, CRM_LEAD_INVALID_STATUS, CRM_BANT_INVALID, CRM_QUAL_INCOMPLETE
- [ ] Unit tests: `LeadQualificationServiceTest.java`
- [ ] Integration test: `LeadQualificationControllerTest.java`

### Frontend Generated:
- [ ] `lead-qualification-page.tsx`
- [ ] `bant-criteria-form.tsx` component
- [ ] `score-result-display.tsx` component
- [ ] `score-override-modal.tsx` component
- [ ] `qualification-history-table.tsx` component
- [ ] `useStartQualification.ts` TanStack Query hook
- [ ] `useSubmitBANT.ts` hook
- [ ] `useFinalizeQualification.ts` hook
- [ ] `useQualificationHistory.ts` hook
- [ ] `lead-qualification.types.ts` (DTO types)
- [ ] `bant-criteria-schema.ts` (Zod validation)

### Database:
- [ ] Migration: `V016__create_lead_qualifications.sql`
- [ ] Migration: `V017__add_qualification_indexes.sql`

---

**End of Specification**
