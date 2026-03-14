# Feature Specification Validation Checklist

## Purpose

This checklist ensures Feature Specifications are complete, unambiguous, and AI-ready before handoff to development teams. Use this to validate spec quality and identify missing sections.

---

## Pre-Submission Validation

### ✅ Completeness Check (17 Mandatory Sections)

Run through each section and verify it contains concrete details (no "TBD" or placeholders):

- [ ] **1. Feature Metadata**: Name, ID, version, owner, priority, status, description, business goal filled
- [ ] **2. Actors**: All roles listed with RBAC traceability
- [ ] **3. Business Context**: When, why, system boundaries explicit
- [ ] **4. Business Flow**: Step-by-step workflow with actor actions and system responses
- [ ] **5. API Contract**: All endpoints with request/response JSON examples
- [ ] **6. Validation Rules**: Every input field has validation rule + error code
- [ ] **7. Domain Model**: All entities with fields, types, constraints, indexes
- [ ] **8. State Machine**: States, transitions, guards defined (or N/A if stateless)
- [ ] **9. External Integrations**: Services, endpoints, timeouts, retries, fallbacks
- [ ] **10. Event Flow**: Events defined with payload structure (or N/A if no events)
- [ ] **11. Error Codes**: All error scenarios mapped to specific codes with HTTP status
- [ ] **12. Observability**: Logs, metrics, traces specified
- [ ] **13. Security**: Authentication, authorization, RBAC, data-scope rules
- [ ] **14. Performance**: Response time, throughput, resource limits defined
- [ ] **15. Frontend UI**: Pages, components, interactions, states detailed
- [ ] **16. Test Scenarios**: Unit tests, integration tests, edge cases, failure scenarios
- [ ] **17. AI Output Expectations**: Explicit checklist of files to generate

---

## Section-Level Quality Gates

### Section 1: Feature Metadata

- [ ] Feature ID follows format: `{MODULE}-{NUMBER}` (e.g., `CRM-015`)
- [ ] Business goal is measurable (includes metrics or time savings)
- [ ] Priority is one of: CRITICAL, HIGH, MEDIUM, LOW
- [ ] Status is one of: DRAFT, REVIEW, APPROVED, IMPLEMENTED

---

### Section 2: Actors

- [ ] All actor roles traced to `docs/architecture/rbac-model.md`
- [ ] Each actor has clear responsibility description
- [ ] System actor included if automation involved

---

### Section 3: Business Context

- [ ] "When" describes triggering scenario
- [ ] "Why" explains business problem being solved
- [ ] "System Boundaries" lists in-scope and out-of-scope items explicitly

---

### Section 4: Business Flow

- [ ] Flow starts with user action or triggering event
- [ ] Each step numbered sequentially
- [ ] System responses clearly distinguished from user actions
- [ ] Flow ends with user confirmation or system final state
- [ ] Alternative flows (error scenarios) included

---

### Section 5: API Contract

For **each endpoint**, verify:
- [ ] HTTP method specified (GET, POST, PUT, DELETE, PATCH)
- [ ] Full path with path parameters (e.g., `/api/crm/leads/{leadId}`)
- [ ] Description explains what the endpoint does
- [ ] Request structure:
  - [ ] Content-Type specified (application/json, multipart/form-data, etc.)
  - [ ] JSON example with all fields
  - [ ] Field descriptions (name, type, required/optional, constraints)
- [ ] Response structure:
  - [ ] Success response JSON example (200, 201, 202, 204)
  - [ ] Error response examples (400, 404, 409, 422, 500)
- [ ] HTTP status codes documented with error codes
- [ ] No placeholder values like "..." or "TBD"

---

### Section 6: Validation Rules

For **each input field**, verify:
- [ ] Validation rule specified (required, max length, format, range, etc.)
- [ ] Error code assigned following naming convention: `{MODULE}_{ENTITY}_{FIELD}_{REASON}`
- [ ] Constraint aligns with database schema from `docs/data/database-schema.md`
- [ ] Bean Validation annotations mentioned (@NotBlank, @Email, @Size, @Min, @Max)

---

### Section 7: Domain Model

For **each entity**, verify:
- [ ] Entity name in PascalCase (e.g., `CustomerImportJob`)
- [ ] Table name in snake_case (e.g., `crm_customer_import_jobs`)
- [ ] Primary key: `id UUID`
- [ ] Audit fields present:
  - [ ] `created_at TIMESTAMP NOT NULL`
  - [ ] `created_by UUID NOT NULL`
  - [ ] `updated_at TIMESTAMP NULL`
  - [ ] `updated_by UUID NULL`
- [ ] Soft delete fields (if applicable):
  - [ ] `deleted BOOLEAN NOT NULL DEFAULT FALSE`
  - [ ] `deleted_at TIMESTAMP NULL`
- [ ] All fields have type + constraints (NOT NULL, UNIQUE, FK, etc.)
- [ ] Indexes specified with naming convention: `idx_{table}_{column(s)}`
- [ ] Relationships defined (FK references)

---

### Section 8: State Machine

If feature has workflow states:
- [ ] All states listed
- [ ] Allowed transitions mapped with arrows (STATE_A → STATE_B)
- [ ] Forbidden transitions explicitly called out
- [ ] Permission guards specified (who can trigger each transition)
- [ ] Terminal states identified (no outbound transitions)

If feature is stateless:
- [ ] Section marked as "N/A - This feature has no workflow state machine"

---

### Section 9: External Integrations

For **each external service**, verify:
- [ ] Service name
- [ ] Endpoint (full path or pattern)
- [ ] Purpose (why is this integration needed)
- [ ] Timeout value (seconds)
- [ ] Retry policy (attempts + backoff strategy)
- [ ] Fallback behavior (what happens if service fails)

---

### Section 10: Event Flow

If feature publishes events:
- [ ] Event naming convention followed: `{entity}.{action}.{past_tense}` (e.g., `lead.qualification.completed`)
- [ ] Payload structure defined with JSON example
- [ ] "When" specifies triggering condition
- [ ] Subscribers listed (which services consume this event)

If feature does not publish events:
- [ ] Section marked as "N/A - This is a read-only/synchronous feature with no events"

---

### Section 11: Error Codes

For **each error code**, verify:
- [ ] Follows naming convention: `{MODULE}_{CATEGORY}_{DESCRIPTOR}` (e.g., `CRM_CUSTOMER_NOT_FOUND`)
- [ ] HTTP status code appropriate (400 validation, 404 not found, 409 conflict, 422 business rule, 500 server error)
- [ ] Message in English (will be translated in implementation)
- [ ] "When" column describes triggering condition
- [ ] All error codes traceable to error system: `.cursor/rules/spring-nextjs-multi-language-error-system.mdc`

---

### Section 12: Observability

**Logs:**
- [ ] Key log events listed (e.g., `feature_started`, `feature_completed`, `feature_failed`)
- [ ] Log level specified (DEBUG, INFO, WARN, ERROR)
- [ ] Structured JSON format example provided
- [ ] `trace_id` included in log payload

**Metrics:**
- [ ] Custom metrics listed (counters, gauges, histograms)
- [ ] Metric naming convention followed: `{module}_{entity}_{metric}_{unit}` (e.g., `customer_import_duration_seconds`)
- [ ] Business KPI metrics included (success rate, throughput, etc.)

**Tracing:**
- [ ] Span names listed (e.g., `uploadCustomerImportFile`, `processImportJob`)
- [ ] Parent-child span relationships described
- [ ] Span attributes listed (e.g., `job_id`, `total_rows`, `success_count`)

**Standards Alignment:**
- [ ] Observability requirements follow `.cursor/rules/spring-nextjs-observability-tracing-system.mdc`

---

### Section 13: Security Requirements

**Authentication:**
- [ ] JWT token requirement stated
- [ ] Token expiration handling mentioned

**Authorization (RBAC):**
- [ ] Permission matrix table provided (endpoint → required permission → data scope)
- [ ] Permissions traced to `docs/architecture/rbac-model.md`
- [ ] Data-scope rules specified: OWN, TEAM, ALL
- [ ] Special authorization rules documented (e.g., manager approval for high-value conversions)

**Input Validation:**
- [ ] SQL injection prevention (parameterized queries)
- [ ] XSS prevention (input sanitization)
- [ ] CSRF protection (for state-changing operations)
- [ ] File upload security (extension whitelist, size limits, virus scan)

**Data Protection:**
- [ ] Sensitive data handling (PII masking, encryption at rest)
- [ ] Audit logging for sensitive operations

---

### Section 14: Performance Requirements

- [ ] Response time targets (p95) specified per endpoint type
- [ ] Throughput targets (requests per second, concurrent users)
- [ ] Resource limits defined (max file size, max rows, max payload)
- [ ] Timeout configuration specified (API calls, background jobs)
- [ ] Caching strategy mentioned (if applicable)
- [ ] Database optimization notes (indexes, query optimization)

---

### Section 15: Frontend UI Specification

**For each page/component:**
- [ ] Route path specified (e.g., `/crm/customers/import`)
- [ ] Access control (which roles can view)
- [ ] Component names listed in PascalCase
- [ ] Form fields defined with types (text input, dropdown, checkbox, date picker, etc.)
- [ ] User interactions described (click, submit, cancel, etc.)
- [ ] UI states specified:
  - [ ] Loading state (spinner, progress bar)
  - [ ] Success state (confirmation message, navigation)
  - [ ] Error state (error message display, retry option)
- [ ] Client-side validation rules listed
- [ ] Error handling behavior specified (toast, inline error, modal)

---

### Section 16: Test Scenarios

**Unit Tests:**
- [ ] Backend service unit tests listed (test method names)
- [ ] Frontend component unit tests listed
- [ ] Test coverage includes: happy path, validation failures, edge cases

**Integration Tests:**
- [ ] API integration tests listed (end-to-end flow)
- [ ] External service integration tests (mocked services)

**Failure Scenarios:**
- [ ] Database failure handling
- [ ] External service timeout handling
- [ ] Concurrent request handling
- [ ] Invalid input handling

**Edge Cases:**
- [ ] Empty input
- [ ] Maximum limits (file size, row count, string length)
- [ ] Duplicate submissions
- [ ] Expired sessions

---

### Section 17: AI Output Expectations

**Backend Checklist:**
- [ ] Entity class names listed (e.g., `CustomerImportJob.java`)
- [ ] Repository interface names listed
- [ ] Service class names listed
- [ ] Controller class names listed
- [ ] DTO class names listed (Request/Response pairs)
- [ ] Error code enum additions specified
- [ ] Unit test class names listed
- [ ] Integration test class names listed
- [ ] Database migration file name specified (e.g., `V015__create_customer_import_tables.sql`)

**Frontend Checklist:**
- [ ] Page component paths listed (e.g., `app/crm/customers/import/page.tsx`)
- [ ] Component file names listed
- [ ] API hook names listed (e.g., `useUploadCustomerImport.ts`)
- [ ] Type definition file names listed
- [ ] Validation schema file names listed (Zod)
- [ ] Test file names listed

**Estimated Coverage:**
- [ ] AI code generation coverage percentage stated (target: 90-95%)

---

## Architecture Standards Compliance

### Traceability to Existing Standards

- [ ] **Architecture Decisions**: References to AD-001 through AD-014 from `docs/summary.md`
- [ ] **RBAC Model**: Permissions aligned with `docs/architecture/rbac-model.md`
- [ ] **Database Schema**: Follows conventions from `docs/data/database-schema.md` (UUID PK, audit fields, soft delete)
- [ ] **Error System**: Error codes follow `.cursor/rules/spring-nextjs-multi-language-error-system.mdc`
- [ ] **Observability**: Logging/metrics/tracing follow `.cursor/rules/spring-nextjs-observability-tracing-system.mdc`
- [ ] **API Standards**: REST conventions follow `.cursor/rules/spring-api-rules.mdc`
- [ ] **Null Safety**: Validation aligned with `.cursor/rules/spring-null-safety.mdc`

---

## Anti-Pattern Detection

### ❌ Reject Spec If:

- [ ] Any section contains "TBD", "to be defined", "will be decided later"
- [ ] API contracts missing request or response JSON examples
- [ ] Domain model missing audit fields (created_at, created_by, updated_at, updated_by)
- [ ] State machine for stateful entity is missing or incomplete
- [ ] Error codes use generic names like "ERROR_001" instead of descriptive codes
- [ ] Observability section omits logs, metrics, or traces
- [ ] Security section doesn't specify RBAC permissions
- [ ] Frontend UI section doesn't define UI states (loading, success, error)
- [ ] Test scenarios only cover happy path (no failure scenarios)
- [ ] AI Output Expectations section is vague (no file names listed)

---

## Final Quality Score

### Scoring Rubric

**Completeness (40 points):**
- All 17 sections filled: 40 points
- 1-2 sections incomplete: 30 points
- 3+ sections incomplete: 0 points (reject)

**Clarity (30 points):**
- All API contracts have JSON examples: 10 points
- All domain entities fully defined: 10 points
- All workflows have step-by-step flow: 10 points

**Architecture Compliance (20 points):**
- Traceable to AD-001..014: 5 points
- RBAC aligned: 5 points
- Error codes standardized: 5 points
- Observability requirements clear: 5 points

**AI-Readiness (10 points):**
- File names explicitly listed: 5 points
- Test scenarios comprehensive: 5 points

**Pass Threshold:** ≥ 85 points (out of 100)

---

## Validation Report Template

Use this template to report validation results:

```markdown
# Feature Specification Validation Report

**Feature ID:** CRM-015  
**Feature Name:** Customer Bulk Import  
**Validated By:** [Your Name]  
**Validation Date:** 2026-03-13

## Validation Score: 92/100 ✅ PASS

### Completeness: 40/40
- All 17 sections complete

### Clarity: 28/30
- API contracts: ✅ Complete
- Domain model: ✅ Complete
- Business flow: ⚠️ Step 7 could be more explicit

### Architecture Compliance: 18/20
- AD references: ✅ Complete
- RBAC alignment: ✅ Complete
- Error codes: ✅ Complete
- Observability: ⚠️ Missing custom metric for throughput

### AI-Readiness: 10/10
- File checklist: ✅ Complete
- Test scenarios: ✅ Complete

## Recommendations:
1. Add more detail to business flow Step 7 (user review errors)
2. Add custom metric: `customer_import_throughput_rows_per_second`

## Approval Status: ✅ APPROVED with minor revisions
```

---

## Automated Validation Script (Future Enhancement)

For v2, consider building a script that:
1. Parses Feature Spec markdown file
2. Checks for presence of all 17 section headers
3. Validates JSON examples are well-formed
4. Checks error code naming convention with regex
5. Outputs validation report automatically

**Example Usage:**
```bash
./validate-spec.sh docs/features/crm-015-customer-bulk-import.md
# Output:
# ✅ Section 1: Feature Metadata - Complete
# ✅ Section 2: Actors - Complete
# ...
# ⚠️ Section 11: Error Codes - CRM_400 doesn't follow naming convention
# Score: 88/100 - PASS
```

---

## Version History

| Version | Date | Changes | Author |
|---|---|---|---|
| v1.0 | 2026-03-13 | Initial validation checklist for BA Feature Specifications | BA Enhancement Project |
