# Feature Specification: Opportunity Forecast Dashboard

## 1. Feature Metadata

**Feature Name:** Opportunity Forecast Dashboard  
**Feature ID:** CRM-017  
**Version:** v1.0  
**Owner Team:** Fullstack  
**Priority:** HIGH  
**Status:** APPROVED

**Description:**
Real-time analytics dashboard displaying sales pipeline forecast with filtering by stage, owner, date range, and team. Features include weighted forecast calculation, win probability analysis, pipeline health metrics, trend charts, and downloadable reports. Designed for SALES_MANAGER and CRM_ADMIN to monitor team performance and forecast accuracy.

**Business Goal:**
Improve forecast accuracy from 65% to 85% by providing visibility into pipeline health, stage velocity, and win probability trends. Enable managers to identify at-risk deals early and adjust sales strategies proactively.

---

## 2. Actors

| Actor | Role Code | Description |
|---|---|---|
| Sales Manager | SALES_MANAGER | Views team forecast, analyzes pipeline trends, downloads reports |
| CRM Administrator | CRM_ADMIN | Views all forecasts, configures probability weights, analyzes org-wide metrics |
| Sales Representative | SALES_REP | Views own forecast only (read-only dashboard) |

**RBAC Traceability:**
- Permission: `opportunity:read` (from `docs/architecture/rbac-model.md`)
- Data Scope: SALES_REP (OWN), SALES_MANAGER (TEAM), CRM_ADMIN (ALL)

---

## 3. Business Context

**When:**
- Weekly forecast review meetings (every Monday 9 AM)
- Month-end sales planning and quota tracking
- Executive asks "What's our Q2 forecast?"

**Why:**
- Manual spreadsheet forecasts are error-prone and outdated
- Lack of visibility into pipeline health causes missed quotas
- Managers need real-time data to coach reps on at-risk deals

**System Boundaries:**
- **In Scope:** crm-service (opportunity aggregation), analytics-service (trend calculations), report-service (PDF/CSV export)
- **Out of Scope:** AI-powered win probability (v2), integration with accounting systems (v2), mobile dashboard (v2)

---

## 4. Business Flow

**Step 1:** Manager navigates to `/crm/forecast` dashboard  
**Step 2:** System loads default view: Current quarter, all team members, all stages  
**Step 3:** System calculates weighted forecast: Σ(opportunity_value × stage_probability)  
**Step 4:** Dashboard displays 4 KPI cards:
- Total Pipeline Value: $2.4M
- Weighted Forecast: $1.6M (67% probability)
- Expected Wins This Quarter: 24 opportunities
- Pipeline Health Score: 78/100 (green)  
**Step 5:** Manager applies filters: Stage = "Proposal", Owner = "John Doe", Date Range = "Next 30 days"  
**Step 6:** System re-calculates metrics and updates charts  
**Step 7:** Manager views "Forecast Trend" line chart showing 12-week weighted forecast history  
**Step 8:** Manager clicks "Stage Velocity" bar chart → Tooltip shows avg days in each stage  
**Step 9:** Manager clicks "Download Report" → Selects format (PDF or CSV)  
**Step 10:** System generates report with current filters applied → Downloads file  
**Step 11:** Manager shares report in weekly review meeting

---

## 5. API Contract

### 5.1 Get Forecast Summary

**Endpoint:** `GET /api/crm/forecast/summary`  
**Method:** GET  
**Description:** Retrieve forecast KPIs with filtering

**Query Parameters:**
- `quarter` (string, optional): "Q1-2026", "Q2-2026", default: current quarter
- `stage` (string, optional): Comma-separated stages, default: all stages
- `owner_user_id` (UUID, optional): Filter by opportunity owner
- `team_id` (UUID, optional): Filter by team (SALES_MANAGER scope)

**Response:**
```json
{
  "quarter": "Q2-2026",
  "filters_applied": {
    "stage": ["PROPOSAL", "NEGOTIATION"],
    "owner_user_id": "user-uuid-123",
    "team_id": null
  },
  "kpis": {
    "total_pipeline_value": 2400000.00,
    "weighted_forecast": 1600000.00,
    "average_win_probability": 67,
    "expected_wins_count": 24,
    "pipeline_health_score": 78,
    "total_opportunities": 45
  },
  "stage_breakdown": [
    {
      "stage": "PROPOSAL",
      "count": 15,
      "total_value": 800000.00,
      "weighted_value": 400000.00,
      "win_probability": 50
    },
    {
      "stage": "NEGOTIATION",
      "count": 10,
      "total_value": 600000.00,
      "weighted_value": 480000.00,
      "win_probability": 80
    }
  ],
  "generated_at": "2026-03-13T15:00:00Z"
}
```

**Status Codes:**
- 200: Forecast retrieved
- 400: Invalid quarter format (CRM_FORECAST_INVALID_QUARTER)
- 403: Insufficient permissions for requested scope

---

### 5.2 Get Forecast Trend

**Endpoint:** `GET /api/crm/forecast/trend`  
**Method:** GET  
**Description:** Retrieve 12-week weighted forecast trend

**Query Parameters:**
- `weeks` (integer, optional): Number of historical weeks, default: 12, max: 52

**Response:**
```json
{
  "weeks": 12,
  "data_points": [
    {
      "week_start_date": "2026-01-06",
      "week_number": 1,
      "total_pipeline_value": 2100000.00,
      "weighted_forecast": 1400000.00,
      "opportunities_count": 38
    },
    {
      "week_start_date": "2026-01-13",
      "week_number": 2,
      "total_pipeline_value": 2200000.00,
      "weighted_forecast": 1500000.00,
      "opportunities_count": 40
    }
  ]
}
```

**Status Codes:**
- 200: Trend data retrieved
- 400: Invalid weeks parameter (CRM_FORECAST_INVALID_WEEKS)

---

### 5.3 Get Stage Velocity

**Endpoint:** `GET /api/crm/forecast/stage-velocity`  
**Method:** GET  
**Description:** Calculate average days opportunities spend in each stage

**Response:**
```json
{
  "stages": [
    {
      "stage": "NEW",
      "average_days": 7,
      "median_days": 5,
      "opportunities_count": 50
    },
    {
      "stage": "QUALIFIED",
      "average_days": 14,
      "median_days": 12,
      "opportunities_count": 45
    },
    {
      "stage": "PROPOSAL",
      "average_days": 21,
      "median_days": 18,
      "opportunities_count": 30
    },
    {
      "stage": "NEGOTIATION",
      "average_days": 10,
      "median_days": 8,
      "opportunities_count": 20
    },
    {
      "stage": "CLOSING",
      "average_days": 5,
      "median_days": 4,
      "opportunities_count": 15
    }
  ],
  "calculated_at": "2026-03-13T15:00:00Z"
}
```

**Status Codes:**
- 200: Velocity data retrieved

---

### 5.4 Download Forecast Report

**Endpoint:** `POST /api/crm/forecast/reports/generate`  
**Method:** POST  
**Description:** Generate and download forecast report (PDF or CSV)

**Request:**
```json
{
  "format": "PDF",
  "filters": {
    "quarter": "Q2-2026",
    "stage": ["PROPOSAL", "NEGOTIATION"],
    "owner_user_id": "user-uuid-123"
  },
  "include_charts": true
}
```

**Response:**
```json
{
  "report_id": "report-uuid-001",
  "download_url": "/api/crm/forecast/reports/report-uuid-001/download",
  "format": "PDF",
  "file_size_bytes": 245680,
  "expires_at": "2026-03-13T16:00:00Z",
  "generated_at": "2026-03-13T15:05:00Z"
}
```

**Status Codes:**
- 201: Report generated
- 400: Invalid format or filters (CRM_FORECAST_REPORT_INVALID)

---

## 6. Validation Rules

| Field | Rule | Error Code |
|---|---|---|
| quarter | Format: Q[1-4]-YYYY, year 2020-2030 | CRM_FORECAST_INVALID_QUARTER |
| weeks | Integer, range 1-52 | CRM_FORECAST_INVALID_WEEKS |
| format | Enum: PDF, CSV | CRM_FORECAST_REPORT_INVALID_FORMAT |
| stage | Valid opportunity stage values | CRM_FORECAST_INVALID_STAGE |
| owner_user_id | Must reference active user | CRM_FORECAST_INVALID_OWNER |

---

## 7. Domain Model

### 7.1 OpportunityForecastSnapshot

**Table:** `crm_opportunity_forecast_snapshots`

| Field | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PK | Snapshot identifier |
| snapshot_date | DATE | NOT NULL | Date of snapshot (weekly) |
| quarter | VARCHAR(10) | NOT NULL | Q1-2026, Q2-2026, etc. |
| total_pipeline_value | DECIMAL(15,2) | NOT NULL | Sum of all opp values |
| weighted_forecast | DECIMAL(15,2) | NOT NULL | Probability-weighted sum |
| opportunities_count | INTEGER | NOT NULL | Total opps in snapshot |
| created_at | TIMESTAMP | NOT NULL | Audit |

**Indexes:**
- `idx_forecast_snapshots_date` (snapshot_date DESC)
- `idx_forecast_snapshots_quarter` (quarter)

---

### 7.2 OpportunityStageVelocity (Materialized View)

**View:** `vw_opportunity_stage_velocity`

Calculates average days in each stage based on opportunity_stage_history table.

```sql
CREATE VIEW vw_opportunity_stage_velocity AS
SELECT 
  stage,
  AVG(days_in_stage) as average_days,
  PERCENTILE_CONT(0.5) WITHIN GROUP (ORDER BY days_in_stage) as median_days,
  COUNT(*) as opportunities_count
FROM opportunity_stage_history
WHERE transition_date >= CURRENT_DATE - INTERVAL '90 days'
GROUP BY stage;
```

---

## 8. State Machine

*N/A - This is an analytics/reporting feature with no workflow state machine.*

---

## 9. External Integrations

### 9.1 Analytics Service

**Service:** analytics-service  
**Endpoint:** `GET /api/analytics/opportunity-trends`  
**Purpose:** Historical trend data for forecast accuracy analysis  
**Timeout:** 5 seconds  
**Retry:** 2 attempts  
**Fallback:** Show current data only, hide trend chart

---

### 9.2 Report Service

**Service:** report-service  
**Endpoint:** `POST /api/reports/generate`  
**Purpose:** Generate PDF/CSV reports with charts  
**Timeout:** 30 seconds  
**Retry:** 1 attempt  
**Fallback:** Return error, suggest manual export

---

## 10. Event Flow

*N/A - This is a read-only dashboard feature with no events published.*

---

## 11. Error Codes

| Error Code | HTTP Status | Message (English) | When |
|---|---|---|---|
| CRM_FORECAST_INVALID_QUARTER | 400 | Invalid quarter format. Expected Q[1-4]-YYYY. | Malformed quarter string |
| CRM_FORECAST_INVALID_WEEKS | 400 | Weeks parameter must be between 1 and 52. | Out of range |
| CRM_FORECAST_INVALID_STAGE | 400 | Invalid opportunity stage: {stage} | Unknown stage value |
| CRM_FORECAST_INVALID_OWNER | 400 | Owner user ID not found or inactive. | owner_user_id FK violation |
| CRM_FORECAST_REPORT_INVALID | 400 | Invalid report format or filters. | Bad request payload |
| CRM_FORECAST_REPORT_GENERATION_FAILED | 500 | Report generation failed. Please try again. | Report service error |

---

## 12. Observability

### Logs

**Log Events:**
- `forecast_dashboard_loaded` (INFO): user_id, filters_applied
- `forecast_report_requested` (INFO): format, filters
- `forecast_report_generated` (INFO): report_id, file_size, duration

**Example:**
```json
{
  "timestamp": "2026-03-13T15:00:00Z",
  "level": "INFO",
  "service": "crm-service",
  "message": "forecast_dashboard_loaded",
  "trace_id": "trace-xyz-789",
  "user_id": "user-uuid-manager",
  "filters": {"quarter": "Q2-2026", "stage": ["PROPOSAL"]}
}
```

---

### Metrics

- `forecast_dashboard_views_total` (Counter): Dashboard page views
- `forecast_report_downloads_total` (Counter): Reports downloaded
- `forecast_calculation_duration_seconds` (Histogram): Time to calculate weighted forecast
- `forecast_api_response_time_seconds` (Histogram): /forecast/summary endpoint latency

---

### Tracing

**Spans:**
- `getForecastSummary`: Main calculation (300-500ms)
  - Child: `aggregateOpportunities` (200ms)
  - Child: `calculateWeightedForecast` (100ms)
- `generateForecastReport`: PDF/CSV generation (5-10s)

---

## 13. Security Requirements

### Authentication
- All endpoints require valid JWT token

### Authorization (RBAC)

| Endpoint | Required Permission | Data Scope |
|---|---|---|
| GET /forecast/summary | opportunity:read | OWN (SALES_REP), TEAM (SALES_MANAGER), ALL (CRM_ADMIN) |
| GET /forecast/trend | opportunity:read | OWN/TEAM/ALL |
| GET /forecast/stage-velocity | opportunity:read | OWN/TEAM/ALL |
| POST /forecast/reports/generate | opportunity:read | OWN/TEAM/ALL |

**Data Scope Enforcement:**
- SALES_REP: Automatically filter owner_user_id = current_user_id
- SALES_MANAGER: Filter by team_id (user's managed team)
- CRM_ADMIN: No filter, see all data

### Input Validation
- Whitelist quarter format with regex: `^Q[1-4]-\d{4}$`
- Cap weeks parameter at 52 to prevent expensive queries
- Sanitize CSV output to prevent CSV injection

---

## 14. Performance Requirements

### Response Time (p95)
- Forecast summary: < 500ms (aggregation query optimized with indexes)
- Trend data: < 800ms (pre-computed weekly snapshots)
- Stage velocity: < 300ms (materialized view)
- Report generation: < 15 seconds (PDF with charts)

### Throughput
- Support 200 concurrent dashboard views
- Support 50 report generations per minute

### Caching Strategy
- Forecast summary cached for 5 minutes (Redis)
- Stage velocity cached for 1 hour (changes slowly)
- Trend data cached for 1 day (historical, immutable)

### Database Optimization
- Weekly snapshot job pre-calculates forecast data (reduces query time)
- Composite indexes on (quarter, owner_user_id, stage)
- Materialized view for stage velocity refreshed daily

---

## 15. Frontend UI Specification

### Page: Forecast Dashboard

**Route:** `/crm/forecast`  
**Access:** SALES_REP (own data), SALES_MANAGER (team data), CRM_ADMIN (all data)

---

### Layout: Dashboard Grid

**4 KPI Cards (Top Row):**
1. **Total Pipeline Value**: "$2.4M" (large, blue)
2. **Weighted Forecast**: "$1.6M (67%)" (large, green, tooltip: "Probability-weighted")
3. **Expected Wins**: "24 opportunities" (large, purple)
4. **Pipeline Health**: "78/100" (large, color-coded: <60 red, 60-79 yellow, 80+ green)

---

**Filter Bar (Below KPIs):**
- Quarter Dropdown: "Q2-2026" (current quarter default)
- Stage Multi-Select: "All Stages" (Proposal, Negotiation, Closing options)
- Owner Dropdown: "All Team Members" (SALES_MANAGER sees team, SALES_REP disabled)
- Date Range Picker: "Next 30 days" (quick select: 7, 30, 60, 90 days)
- "Reset Filters" link

---

**Chart Row 1:**
- **Forecast Trend Line Chart** (full width):
  - X-axis: 12 weeks (dates)
  - Y-axis: Weighted Forecast ($)
  - Line: Weighted forecast per week
  - Area fill: Total pipeline value (lighter shade)
  - Hover tooltip: "Week of Jan 6: $1.4M forecast, $2.1M pipeline"

---

**Chart Row 2 (2 columns):**
- **Stage Breakdown Funnel Chart** (left):
  - Stages: NEW (45) → QUALIFIED (30) → PROPOSAL (15) → NEGOTIATION (10) → CLOSING (5)
  - Width proportional to count
  - Click stage → filter dashboard to that stage
  
- **Stage Velocity Bar Chart** (right):
  - X-axis: Stages
  - Y-axis: Average days
  - Bars: Color-coded (green <10, yellow 10-20, red >20 days)
  - Tooltip: "Proposal: 21 days avg, 18 days median"

---

**Actions (Bottom):**
- "Download Report" button (primary, opens modal)
- "Email Report" button (secondary, future feature)
- "Schedule Weekly Report" link (future feature)

---

### Component: ReportDownloadModal

**File:** `app/crm/forecast/components/report-download-modal.tsx`

**Fields:**
- Format: Radio buttons (PDF with charts, CSV data only)
- Include filters: Checkbox "Apply current filters to report" (checked by default)

**Actions:**
- "Generate & Download" button
- "Cancel" button

**States:**
- Idle: Form enabled
- Generating: Progress bar "Generating report... 45%"
- Success: Auto-download file, close modal
- Error: Show error message, retry button

---

## 16. Test Scenarios

### Unit Tests

#### Backend: ForecastService

- `shouldCalculateWeightedForecastCorrectly()`: 3 opps with different probabilities → verify weighted sum
- `shouldApplyDataScopeForSalesRep()`: SALES_REP user → only own opps included
- `shouldApplyTeamScopeForSalesManager()`: SALES_MANAGER user → team opps included
- `shouldCalculateStageVelocity()`: Opps with stage history → verify avg days per stage
- `shouldGenerateWeeklySnapshot()`: Scheduled job → snapshot created with correct KPIs

---

#### Frontend: ForecastDashboard

- `shouldLoadKPIsOnMount()`: Page load → GET /forecast/summary called
- `shouldUpdateChartsWhenFiltersChange()`: Change quarter filter → charts re-render
- `shouldDisplayCorrectDataScopeForRole()`: SALES_REP sees own data, SALES_MANAGER sees team
- `shouldDownloadReportSuccessfully()`: Click download → POST /reports/generate → file downloads

---

### Integration Tests

#### API Integration

- GET /forecast/summary with filters → Verify correct opp aggregation
- POST /reports/generate PDF → Verify file generated with charts

---

### Performance Tests

- Load 1000 opportunities → Forecast summary responds < 500ms
- 200 concurrent dashboard users → All requests < 1s

---

## 17. AI Output Expectations

### Backend Files

#### Services
- [ ] `ForecastService.java` (KPI calculation, aggregation)
- [ ] `ForecastReportService.java` (PDF/CSV generation)
- [ ] `ForecastSnapshotScheduler.java` (weekly snapshot job)

#### Controllers
- [ ] `ForecastController.java` (4 endpoints)

#### DTOs
- [ ] `ForecastSummaryResponse.java`
- [ ] `ForecastTrendResponse.java`
- [ ] `StageVelocityResponse.java`
- [ ] `GenerateReportRequest.java`
- [ ] `ReportDownloadResponse.java`

#### Tests
- [ ] `ForecastServiceTest.java`

#### Database
- [ ] `V017__create_forecast_snapshot_table.sql`
- [ ] `V017_01__create_stage_velocity_view.sql`

---

### Frontend Files

#### Pages
- [ ] `app/crm/forecast/page.tsx`

#### Components
- [ ] `app/crm/forecast/components/kpi-cards.tsx`
- [ ] `app/crm/forecast/components/forecast-trend-chart.tsx`
- [ ] `app/crm/forecast/components/stage-funnel-chart.tsx`
- [ ] `app/crm/forecast/components/stage-velocity-chart.tsx`
- [ ] `app/crm/forecast/components/forecast-filters.tsx`
- [ ] `app/crm/forecast/components/report-download-modal.tsx`

#### Hooks
- [ ] `hooks/api/useForecastSummary.ts`
- [ ] `hooks/api/useForecastTrend.ts`
- [ ] `hooks/api/useStageVelocity.ts`
- [ ] `hooks/api/useGenerateReport.ts`

#### Types
- [ ] `types/forecast.types.ts`

#### Tests
- [ ] `components/kpi-cards.test.tsx`
- [ ] `components/forecast-trend-chart.test.tsx`

---

**Total Files: 23+ | AI Coverage: 91%**

---

## Version History

| Version | Date | Changes | Author |
|---|---|---|---|
| v1.0 | 2026-03-13 | Initial forecast dashboard specification | BA Team |
