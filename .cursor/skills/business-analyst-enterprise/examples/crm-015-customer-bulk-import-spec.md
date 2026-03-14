# Feature Specification: Customer Bulk Import

## 1. Feature Metadata

**Feature Name:** Customer Bulk Import  
**Feature ID:** CRM-015  
**Version:** v1.0  
**Owner Team:** Fullstack  
**Priority:** HIGH  
**Status:** APPROVED

**Description:**
Enable CRM administrators to import multiple customer records via CSV file upload. The feature supports client-side file validation, server-side row validation, duplicate detection, and asynchronous batch processing. Administrators can review validation errors before proceeding with the import, and receive notifications upon completion.

**Business Goal:**
Reduce customer onboarding time from 5 minutes per record (manual entry) to <10 seconds per record (bulk import), enabling sales teams to migrate legacy CRM data and onboard partner customer lists efficiently. Target: Import 500+ customers in <2 minutes with <5% failure rate.

---

## 2. Actors

| Actor | Role Code | Description |
|---|---|---|
| CRM Administrator | CRM_ADMIN | Initiates CSV upload, reviews validation errors, executes batch import, views import history |
| System Scheduler | SYSTEM | Processes import job asynchronously in background worker |
| Sales Manager | SALES_MANAGER | Views import history and success/failure metrics for reporting |

**RBAC Traceability:**
- Permission required: `customer:create` (from `docs/architecture/rbac-model.md`)
- Data scope: ALL (CRM_ADMIN can import customers for any owner_user_id)

---

## 3. Business Context

**When:**
- CRM Admin receives customer list (CSV) from legacy CRM migration
- Partner provides bulk customer data for integration
- Sales team needs to populate CRM with trade show lead contacts

**Why:**
- Manual entry of 500+ customers takes 40+ hours
- Bulk import reduces this to <1 hour including validation and error review
- Enables rapid CRM adoption and legacy system decommissioning

**System Boundaries:**
- **In Scope:** crm-service (customer creation, validation), file-service (upload/storage), notification-service (completion alerts), auth-service (user validation for owner_user_id)
- **Out of Scope:** Real-time streaming import (batch only), Excel macro support, custom field mapping UI (v2 feature), auto-merge duplicate records (v2 feature)

---

## 4. Business Flow

**Step 1:** User (CRM_ADMIN) navigates to `/crm/customers/import` and selects CSV file via file picker  
**Step 2:** Frontend validates file (size <10MB, extension .csv) before upload  
**Step 3:** User clicks "Upload & Validate" → Frontend POSTs file to `/api/crm/customers/import/upload`  
**Step 4:** Backend validates CSV structure (required columns present, encoding UTF-8)  
**Step 5:** Backend parses all rows and performs validation:
- customer_code: unique, alphanumeric, max 50 chars
- full_name: required, max 200 chars
- email: valid format if provided
- owner_user_id: must reference active user  
**Step 6:** Backend returns validation summary: total_rows, valid_rows, invalid_rows, duplicate_rows, validation_errors[]  
**Step 7:** Frontend displays validation summary table with expandable error details  
**Step 8:** User reviews errors and chooses: "Fix & Re-upload" OR "Proceed with Valid Rows Only"  
**Step 9:** User clicks "Execute Import" → Frontend POSTs to `/api/crm/customers/import/execute`  
**Step 10:** Backend creates CustomerImportJob (status=QUEUED) and returns 202 Accepted  
**Step 11:** Background worker picks up job, transitions status to PROCESSING  
**Step 12:** Worker processes rows in batches of 100, creating Customer entities  
**Step 13:** Worker updates job progress (processed_rows, success_count, failure_count) every batch  
**Step 14:** Frontend polls `/api/crm/customers/import/jobs/{id}` every 3 seconds to show progress bar  
**Step 15:** Upon completion, worker transitions job to COMPLETED or PARTIAL_SUCCESS  
**Step 16:** Notification service sends email to job initiator with summary  
**Step 17:** User clicks "View Details" to see full import report with downloadable error CSV

---

## 5. API Contract

### 5.1 Upload Customer Import File

**Endpoint:** `POST /api/crm/customers/import/upload`  
**Method:** POST (multipart/form-data)  
**Description:** Upload CSV file and validate format and row contents

**Request:**
```
Content-Type: multipart/form-data
--boundary
Content-Disposition: form-data; name="file"; filename="customers.csv"
Content-Type: text/csv

customer_code,full_name,email,phone,owner_user_id,status
CUST001,ABC Corporation,contact@abc.com,+1234567890,uuid-user-1,ACTIVE
CUST002,XYZ Ltd,info@xyz.com,,uuid-user-2,ACTIVE
--boundary--
```

**Response (Success - Validation Complete):**
```json
{
  "upload_id": "550e8400-e29b-41d4-a716-446655440000",
  "file_name": "customers.csv",
  "total_rows": 500,
  "valid_rows": 485,
  "invalid_rows": 10,
  "duplicate_rows": 5,
  "validation_errors": [
    {
      "row": 12,
      "field": "email",
      "code": "CRM_CUSTOMER_EMAIL_INVALID",
      "message": "Invalid email format"
    },
    {
      "row": 45,
      "field": "customer_code",
      "code": "CRM_CUSTOMER_CODE_DUPLICATE",
      "message": "Customer code already exists: CUST045"
    }
  ],
  "status": "VALIDATED",
  "uploaded_at": "2026-03-13T10:00:00Z"
}
```

**Status Codes:**
- 200: Validation complete (even if some rows invalid)
- 400: Invalid file format (CRM_IMPORT_INVALID_FORMAT)
- 413: File too large >10MB (CRM_IMPORT_FILE_TOO_LARGE)
- 422: Missing required columns (CRM_IMPORT_MISSING_COLUMNS)

---

### 5.2 Execute Import Job

**Endpoint:** `POST /api/crm/customers/import/execute`  
**Method:** POST  
**Description:** Create background job to import valid customer rows

**Request:**
```json
{
  "upload_id": "550e8400-e29b-41d4-a716-446655440000",
  "import_mode": "VALID_ONLY"
}
```

**Fields:**
- `upload_id` (UUID, required): Reference to validated upload
- `import_mode` (enum, required): "VALID_ONLY" | "ALL" (if ALL, attempt import even for rows with warnings)

**Response (Job Queued):**
```json
{
  "job_id": "660e8400-e29b-41d4-a716-446655440001",
  "status": "QUEUED",
  "total_rows": 485,
  "estimated_duration_seconds": 120,
  "created_at": "2026-03-13T10:05:00Z"
}
```

**Status Codes:**
- 202: Job queued successfully
- 404: Upload ID not found or expired (CRM_IMPORT_UPLOAD_NOT_FOUND)
- 409: Job already running for this upload (CRM_IMPORT_JOB_DUPLICATE)

---

### 5.3 Get Import Job Status

**Endpoint:** `GET /api/crm/customers/import/jobs/{jobId}`  
**Method:** GET  
**Description:** Retrieve current status and progress of import job

**Response (Processing):**
```json
{
  "job_id": "660e8400-e29b-41d4-a716-446655440001",
  "status": "PROCESSING",
  "total_rows": 485,
  "processed_rows": 250,
  "success_count": 248,
  "failure_count": 2,
  "progress_percentage": 51,
  "started_at": "2026-03-13T10:05:30Z",
  "estimated_completion_at": "2026-03-13T10:07:30Z"
}
```

**Response (Completed):**
```json
{
  "job_id": "660e8400-e29b-41d4-a716-446655440001",
  "status": "COMPLETED",
  "total_rows": 485,
  "processed_rows": 485,
  "success_count": 480,
  "failure_count": 5,
  "progress_percentage": 100,
  "error_report_url": "/api/crm/customers/import/jobs/660e8400-e29b-41d4-a716-446655440001/errors.csv",
  "started_at": "2026-03-13T10:05:30Z",
  "completed_at": "2026-03-13T10:07:15Z",
  "duration_seconds": 105
}
```

**Status Codes:**
- 200: Job found
- 404: Job ID not found (CRM_IMPORT_JOB_NOT_FOUND)

---

### 5.4 Download Error Report

**Endpoint:** `GET /api/crm/customers/import/jobs/{jobId}/errors.csv`  
**Method:** GET  
**Description:** Download CSV file containing failed rows with error reasons

**Response:**
```csv
row,customer_code,full_name,email,error_code,error_message
12,CUST012,Invalid Corp,invalid-email,CRM_CUSTOMER_EMAIL_INVALID,Invalid email format
45,CUST045,Duplicate Inc,contact@dup.com,CRM_CUSTOMER_CODE_DUPLICATE,Customer code already exists
```

**Status Codes:**
- 200: CSV file download
- 404: No errors or job not found

---

## 6. Validation Rules

| Field | Rule | Error Code |
|---|---|---|
| customer_code | Required, unique, max 50 chars, alphanumeric + dash/underscore only | CRM_CUSTOMER_CODE_REQUIRED, CRM_CUSTOMER_CODE_DUPLICATE, CRM_CUSTOMER_CODE_INVALID_FORMAT |
| full_name | Required, max 200 chars | CRM_CUSTOMER_NAME_REQUIRED, CRM_CUSTOMER_NAME_TOO_LONG |
| email | Optional, valid email format if provided, max 255 chars | CRM_CUSTOMER_EMAIL_INVALID |
| phone | Optional, valid phone format if provided, max 50 chars | CRM_CUSTOMER_PHONE_INVALID |
| owner_user_id | Required, must reference active user from auth_users | CRM_CUSTOMER_OWNER_REQUIRED, CRM_CUSTOMER_OWNER_NOT_FOUND |
| status | Must be one of: ACTIVE, INACTIVE | CRM_CUSTOMER_STATUS_INVALID |

**CSV Format Validation:**
- File extension: .csv only
- Encoding: UTF-8
- Max file size: 10MB
- Max rows: 10,000
- Required columns: customer_code, full_name, owner_user_id, status

---

## 7. Domain Model

### 7.1 CustomerImportJob

**Table:** `crm_customer_import_jobs`

| Field | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PK | Job identifier |
| upload_id | UUID | NOT NULL | Reference to file upload metadata |
| initiated_by | UUID | NOT NULL | User ID who started the job |
| file_name | VARCHAR(255) | NOT NULL | Original uploaded file name |
| total_rows | INTEGER | NOT NULL | Total rows in CSV (excluding header) |
| processed_rows | INTEGER | DEFAULT 0 | Rows processed so far |
| success_count | INTEGER | DEFAULT 0 | Successfully imported customers |
| failure_count | INTEGER | DEFAULT 0 | Failed rows |
| status | VARCHAR(30) | NOT NULL | QUEUED, PROCESSING, COMPLETED, PARTIAL_SUCCESS, FAILED |
| error_report_path | VARCHAR(500) | NULL | S3 path to error CSV file |
| started_at | TIMESTAMP | NULL | Job processing start time |
| completed_at | TIMESTAMP | NULL | Job completion time |
| created_at | TIMESTAMP | NOT NULL | Audit: creation time |
| created_by | UUID | NOT NULL | Audit: creator user ID |
| updated_at | TIMESTAMP | NULL | Audit: last update time |
| updated_by | UUID | NULL | Audit: last updater user ID |

**Indexes:**
- `idx_import_jobs_status` (status) - for filtering active jobs
- `idx_import_jobs_initiated_by` (initiated_by) - for user history
- `idx_import_jobs_created_at` (created_at DESC) - for recent jobs query

---

### 7.2 CustomerImportError

**Table:** `crm_customer_import_errors`

| Field | Type | Constraints | Description |
|---|---|---|---|
| id | UUID | PK | Error record identifier |
| job_id | UUID | NOT NULL, FK | Reference to CustomerImportJob |
| row_number | INTEGER | NOT NULL | CSV row number (1-based, excluding header) |
| customer_code | VARCHAR(50) | NULL | Customer code from failed row |
| error_code | VARCHAR(100) | NOT NULL | Standardized error code |
| error_message | VARCHAR(500) | NOT NULL | Human-readable error message |
| row_data_json | TEXT | NULL | Full row data as JSON for debugging |
| created_at | TIMESTAMP | NOT NULL | Error logged at |

**Indexes:**
- `idx_import_errors_job_id` (job_id) - for fetching all errors for a job

---

## 8. State Machine: CustomerImportJob

**States:**
- **QUEUED**: Job accepted, waiting in queue for worker to pick up
- **PROCESSING**: Worker actively creating customer records in batches
- **COMPLETED**: All rows processed successfully (failure_count = 0)
- **PARTIAL_SUCCESS**: Some rows succeeded, some failed (failure_count > 0)
- **FAILED**: Job aborted due to system error (e.g., database connection lost)

**Allowed Transitions:**
- QUEUED → PROCESSING (when background worker picks up job)
- PROCESSING → COMPLETED (all rows succeeded)
- PROCESSING → PARTIAL_SUCCESS (some rows failed)
- PROCESSING → FAILED (critical system error, rollback transaction)

**Forbidden Transitions:**
- COMPLETED → any state (terminal state)
- PARTIAL_SUCCESS → any state (terminal state)
- FAILED → PROCESSING (no automatic retry; user must create new job)

**Permission Guards:**
- Only CRM_ADMIN or job initiator (initiated_by) can view job details
- Only SYSTEM (background worker) can transition job states

---

## 9. External Integrations

### 9.1 File Storage Service

**Service:** file-service  
**Endpoint:** `POST /api/files/upload`  
**Purpose:** Persist uploaded CSV file to S3-compatible storage  
**Timeout:** 10 seconds  
**Retry Policy:** 3 attempts with exponential backoff (1s, 2s, 4s)  
**Fallback:** Return error to user if upload fails; do not proceed with validation

---

### 9.2 User Service (Validation)

**Service:** user-service  
**Endpoint:** `GET /api/users/{id}`  
**Purpose:** Validate owner_user_id exists and is active  
**Timeout:** 3 seconds  
**Retry Policy:** 2 attempts  
**Fallback:** Mark row as invalid with error CRM_CUSTOMER_OWNER_NOT_FOUND

---

### 9.3 Notification Service

**Service:** notification-service  
**Endpoint:** `POST /api/notifications/send`  
**Purpose:** Send email notification to job initiator upon completion  
**Timeout:** 5 seconds  
**Retry Policy:** 3 attempts  
**Fallback:** Log failure, do not block job completion (notifications are non-critical)

---

## 10. Event Flow

### Event: customer.import.job.started

**Payload:**
```json
{
  "event_type": "customer.import.job.started",
  "job_id": "660e8400-e29b-41d4-a716-446655440001",
  "initiated_by": "user-uuid-123",
  "total_rows": 485,
  "timestamp": "2026-03-13T10:05:30Z",
  "trace_id": "abc123def456"
}
```

**When:** Job transitions from QUEUED to PROCESSING  
**Subscribers:** monitoring-service (metrics dashboard), audit-service (compliance log)

---

### Event: customer.import.job.completed

**Payload:**
```json
{
  "event_type": "customer.import.job.completed",
  "job_id": "660e8400-e29b-41d4-a716-446655440001",
  "status": "PARTIAL_SUCCESS",
  "success_count": 480,
  "failure_count": 5,
  "duration_seconds": 105,
  "timestamp": "2026-03-13T10:07:15Z",
  "trace_id": "abc123def456"
}
```

**When:** Job transitions to COMPLETED or PARTIAL_SUCCESS  
**Subscribers:** notification-service (send email alert), analytics-service (success rate tracking)

---

## 11. Error Codes

| Error Code | HTTP Status | Message (English) | When |
|---|---|---|---|
| CRM_IMPORT_INVALID_FORMAT | 400 | Invalid file format. Expected CSV with UTF-8 encoding. | File is not CSV or wrong encoding |
| CRM_IMPORT_FILE_TOO_LARGE | 413 | File size exceeds maximum allowed (10MB). | File > 10MB |
| CRM_IMPORT_MISSING_COLUMNS | 422 | Required CSV columns missing: {columns} | CSV header missing customer_code, full_name, etc. |
| CRM_IMPORT_TOO_MANY_ROWS | 422 | File contains {count} rows. Maximum allowed: 10,000. | Row count > 10,000 |
| CRM_IMPORT_UPLOAD_NOT_FOUND | 404 | Upload ID not found or expired. | upload_id doesn't exist or > 24 hours old |
| CRM_IMPORT_JOB_DUPLICATE | 409 | Import job already running for this upload. | Duplicate execute request |
| CRM_IMPORT_JOB_NOT_FOUND | 404 | Import job not found. | job_id doesn't exist |
| CRM_CUSTOMER_CODE_REQUIRED | 422 | Customer code is required. | Empty customer_code field |
| CRM_CUSTOMER_CODE_DUPLICATE | 422 | Customer code already exists: {code} | Duplicate in DB |
| CRM_CUSTOMER_CODE_INVALID_FORMAT | 422 | Customer code must be alphanumeric with dashes/underscores only. | Invalid characters |
| CRM_CUSTOMER_NAME_REQUIRED | 422 | Customer name is required. | Empty full_name |
| CRM_CUSTOMER_EMAIL_INVALID | 422 | Invalid email format. | Malformed email |
| CRM_CUSTOMER_OWNER_NOT_FOUND | 422 | Owner user ID not found or inactive. | owner_user_id FK violation |
| CRM_CUSTOMER_STATUS_INVALID | 422 | Status must be ACTIVE or INACTIVE. | Invalid status value |

---

## 12. Observability

### Logs (Structured JSON)

**Log Events:**
- `customer_import_upload_received` (INFO): File uploaded, validation started
- `customer_import_validation_completed` (INFO): Validation summary with counts
- `customer_import_job_queued` (INFO): Job created and queued
- `customer_import_job_started` (INFO): Worker started processing
- `customer_import_batch_completed` (DEBUG): Each batch of 100 processed
- `customer_import_job_completed` (INFO): Job finished with final counts
- `customer_import_job_failed` (ERROR): Job aborted with error details

**Example Log:**
```json
{
  "timestamp": "2026-03-13T10:05:30Z",
  "level": "INFO",
  "service": "crm-service",
  "logger": "CustomerImportService",
  "message": "customer_import_job_started",
  "trace_id": "abc123def456",
  "user_id": "user-uuid-123",
  "job_id": "660e8400-e29b-41d4-a716-446655440001",
  "total_rows": 485,
  "batch_size": 100
}
```

---

### Metrics

**Custom Metrics:**
- `customer_import_upload_total` (Counter): Total upload attempts
- `customer_import_upload_success_total` (Counter): Successful validations
- `customer_import_job_duration_seconds` (Histogram): Job processing time distribution
- `customer_import_rows_processed_total` (Counter): Total rows processed across all jobs
- `customer_import_success_rate` (Gauge): success_count / total_rows (per job)
- `customer_import_throughput_rows_per_second` (Gauge): Processing speed

---

### Tracing (OpenTelemetry Spans)

**Spans:**
- `uploadCustomerImportFile`: File upload + validation (duration ~500ms)
  - Child span: `validateCSVStructure` (~50ms)
  - Child span: `validateRowData` (~400ms)
- `executeCustomerImportJob`: Job creation and queueing (~200ms)
- `processCustomerImportJob`: Full job processing (duration ~105s)
  - Child span: `createCustomerBatch` (repeated per batch, ~2s each)

**Span Attributes:**
- `job_id`, `upload_id`, `total_rows`, `batch_size`, `success_count`, `failure_count`

---

## 13. Security Requirements

### Authentication
- All endpoints require valid JWT token in `Authorization: Bearer {token}` header
- Token must not be expired

### Authorization (RBAC)

| Endpoint | Required Permission | Data Scope |
|---|---|---|
| POST /import/upload | customer:create | N/A (upload owned by uploader) |
| POST /import/execute | customer:create | N/A |
| GET /import/jobs | customer:read | OWN (see only own jobs) for SALES_REP, ALL for CRM_ADMIN |
| GET /import/jobs/{id} | customer:read | OWN/ALL per role |
| GET /import/jobs/{id}/errors.csv | customer:read | OWN/ALL per role |

**Role Restrictions:**
- Only CRM_ADMIN can execute bulk imports (customer:create permission)
- SALES_REP cannot access import features (lacks permission)

### Input Validation
- **File upload:** Whitelist .csv extension only
- **CSV content:** Sanitize to prevent CSV injection (e.g., formulas starting with =, +, -, @)
- **SQL injection:** Use parameterized queries for all DB operations
- **Rate limiting:** Max 5 uploads per user per hour (prevent abuse)

### Data Protection
- Uploaded CSV files deleted after 7 days
- Error reports mask sensitive fields for non-admin users (future enhancement)
- Audit log records all import attempts with user_id and timestamp

---

## 14. Performance Requirements

### Response Time (p95)
- File upload endpoint: < 500ms (for 10MB file with 10,000 rows)
- Validation: < 3 seconds (for 10,000 rows)
- Job execute endpoint: < 200ms (async job queued)
- Job status endpoint: < 100ms (simple DB query)

### Throughput
- Support 50 concurrent uploads
- Process 1,000 rows per minute per job (background worker)
- Max concurrent jobs: 10 (configurable via worker pool size)

### Resource Limits
- Max file size: 10MB
- Max rows per file: 10,000
- Batch size for DB insert: 100 rows per transaction
- Upload retention: 7 days

### Timeout Configuration
- File upload timeout: 30 seconds
- Job execution timeout: 30 minutes (fail job if exceeded)
- External API timeout: 10 seconds (file-service, user-service)

---

## 15. Frontend UI Specification

### Page: Customer Import

**Route:** `/crm/customers/import`  
**Access:** CRM_ADMIN only (redirect others to 403 page)

---

### Components

#### 1. FileUploadForm Component

**File:** `app/crm/customers/import/components/file-upload-form.tsx`

**Fields:**
- File input (accept=".csv", max 10MB)
- "Upload & Validate" button (primary, disabled if no file selected)

**States:**
- **Idle**: Show file picker + upload button enabled
- **Uploading**: Show progress bar (0-100%), button disabled, text "Uploading..."
- **Validating**: Show spinner + text "Validating 500 rows..."
- **Validated**: Hide upload form, show ValidationSummary component

**Client-Side Validation:**
- File extension must be .csv (show error toast if wrong)
- File size must be ≤10MB (show error toast if exceeded)

**Interactions:**
- User selects file → update file name display
- User clicks "Upload & Validate" → POST /api/crm/customers/import/upload (multipart/form-data)
- On success → store upload_id, transition to Validated state
- On error → show error toast with translated error message

---

#### 2. ValidationSummary Component

**File:** `app/crm/customers/import/components/validation-summary.tsx`

**Display:**
- File name: "customers.csv"
- Total rows: 500 (neutral badge)
- Valid rows: 485 (green badge with checkmark icon)
- Invalid rows: 10 (red badge with X icon)
- Duplicate rows: 5 (yellow badge with warning icon)

**Error Table (if invalid_rows > 0):**
| Row | Field | Error Message |
|---|---|---|
| 12 | email | Invalid email format |
| 45 | customer_code | Customer code already exists: CUST045 |

**Actions:**
- "Download Error Report" button (secondary, if invalid_rows > 0)
- "Proceed with Valid Rows (485)" button (primary, if valid_rows > 0)
- "Cancel & Re-upload" button (secondary)

**Interactions:**
- Click "Download Error Report" → Generate CSV client-side from validation_errors array
- Click "Proceed" → Show confirmation dialog "Import 485 valid customers? Invalid rows will be skipped."
- Confirm → POST /api/crm/customers/import/execute → Transition to ImportJobStatus component

---

#### 3. ImportJobStatus Component

**File:** `app/crm/customers/import/components/import-job-status.tsx`

**Display (Polling every 3 seconds):**
- Job ID: `660e8400...` (monospace, copy button)
- Status badge: QUEUED (gray) / PROCESSING (blue, pulsing) / COMPLETED (green)
- Progress bar: "250 / 485 rows processed (51%)"
- Success count: 248 (green text)
- Failure count: 2 (red text)
- Elapsed time: "1m 45s"

**Final State (COMPLETED or PARTIAL_SUCCESS):**
- Completion message: "Import completed! 480 customers created, 5 failed."
- "Download Error Report" link (if failure_count > 0)
- "View Imported Customers" button → Navigate to /crm/customers with filter import_job_id={jobId}
- "Start New Import" button → Reset to FileUploadForm

**States:**
- **Polling**: useQuery with refetchInterval=3000ms, enabled when status is QUEUED or PROCESSING
- **Completed**: Stop polling when status is COMPLETED, PARTIAL_SUCCESS, or FAILED

**Error Handling:**
- Network error during polling → Show toast "Connection lost. Retrying..."
- Job status FAILED → Show error alert "Import job failed. Please contact support with Job ID: {jobId}"

---

### UI States Summary

| State | FileUploadForm | ValidationSummary | ImportJobStatus |
|---|---|---|---|
| Initial | Visible | Hidden | Hidden |
| Uploading | Visible (progress) | Hidden | Hidden |
| Validated | Hidden | Visible | Hidden |
| Processing | Hidden | Hidden | Visible (polling) |
| Completed | Hidden | Hidden | Visible (final) |

---

## 16. Test Scenarios

### Unit Tests

#### Backend: CustomerImportService

**Test Class:** `CustomerImportServiceTest.java`

- `shouldValidateCSVFormatSuccess()`: Valid CSV with all required columns → returns validation summary
- `shouldRejectInvalidFileExtension()`: Upload .xlsx file → throws CRM_IMPORT_INVALID_FORMAT
- `shouldRejectFileTooLarge()`: Upload 15MB file → throws CRM_IMPORT_FILE_TOO_LARGE
- `shouldRejectMissingRequiredColumns()`: CSV missing full_name column → throws CRM_IMPORT_MISSING_COLUMNS
- `shouldDetectDuplicateCustomerCodes()`: CSV with duplicate codes → flags in validation_errors[]
- `shouldValidateOwnerUserIdExists()`: owner_user_id references non-existent user → flags error
- `shouldCreateCustomersInBatches()`: Process 500 rows → verify 5 batches of 100 created
- `shouldRollbackOnBatchFailure()`: Database error in batch 3 → rollback batch, continue with batch 4
- `shouldTransitionJobStatusCorrectly()`: QUEUED → PROCESSING → COMPLETED state transitions

---

#### Frontend: FileUploadForm

**Test File:** `file-upload-form.test.tsx`

- `shouldShowErrorForInvalidFileType()`: Select .txt file → display error message
- `shouldShowErrorForFileTooLarge()`: Select 15MB file → display error message
- `shouldDisableUploadButtonWhenNoFile()`: No file selected → button disabled
- `shouldUploadFileSuccessfully()`: Valid file → POST /upload → display ValidationSummary
- `shouldHandleUploadError()`: API returns 400 → display error toast with message

---

#### Frontend: ImportJobStatus

**Test File:** `import-job-status.test.tsx`

- `shouldPollJobStatusEvery3Seconds()`: Status=PROCESSING → verify useQuery refetchInterval=3000
- `shouldStopPollingWhenCompleted()`: Status=COMPLETED → verify polling stopped
- `shouldDisplayProgressBar()`: processed_rows=250, total_rows=500 → 50% progress
- `shouldShowCompletionMessage()`: Status=COMPLETED → display "Import completed!"

---

### Integration Tests

#### API Integration Test

**Test Class:** `CustomerImportIntegrationTest.java`

**Scenario:** End-to-end import flow
1. POST /import/upload with valid CSV → 200 OK, upload_id returned
2. POST /import/execute with upload_id → 202 Accepted, job_id returned
3. Poll GET /import/jobs/{jobId} → status transitions QUEUED → PROCESSING → COMPLETED
4. Query database → verify 485 Customer entities created
5. GET /import/jobs/{jobId}/errors.csv → verify 15 error rows in CSV

---

#### Service Integration Test (External APIs)

**Test Class:** `CustomerImportExternalIntegrationTest.java`

- Mock file-service upload → verify CSV file persisted to S3
- Mock user-service validation → verify owner_user_id lookup called
- Mock notification-service → verify email sent on job completion

---

### Failure Scenarios

#### Error Handling Tests

- **Database connection lost during batch insert:**
  - Transaction rollback
  - Job status set to FAILED
  - Error logged with trace_id
- **External file-service timeout:**
  - Retry 3 times with backoff
  - If all fail, return error to user
- **Duplicate customer_code in database:**
  - Mark row as failed in CustomerImportError
  - Continue processing remaining rows
  - Final status: PARTIAL_SUCCESS

---

#### Edge Cases

- **Upload empty CSV (0 rows):** → return 422 with CRM_IMPORT_TOO_MANY_ROWS (adjust message)
- **Upload CSV with 10,001 rows:** → return 422 with CRM_IMPORT_TOO_MANY_ROWS
- **Execute job with expired upload_id (>24 hours):** → return 404 with CRM_IMPORT_UPLOAD_NOT_FOUND
- **User cancels upload mid-transfer (frontend):** → abort Axios request, cleanup temp file (backend)
- **Concurrent execute requests for same upload_id:** → Second request returns 409 CRM_IMPORT_JOB_DUPLICATE

---

## 17. AI Output Expectations

### Backend Generated Files

#### Entities
- [ ] `CustomerImportJob.java` (JPA entity with audit + soft delete)
- [ ] `CustomerImportError.java` (JPA entity)

#### Repositories
- [ ] `CustomerImportJobRepository.java` (extends JpaRepository)
- [ ] `CustomerImportErrorRepository.java`

#### Services
- [ ] `CustomerImportService.java` (business logic, validation, batch processing)
- [ ] `CustomerImportWorker.java` (background job processor)

#### Controllers
- [ ] `CustomerImportController.java` (3 endpoints: upload, execute, getJobStatus)

#### DTOs
- [ ] `UploadCustomerImportRequest.java` (multipart file)
- [ ] `CustomerImportValidationResponse.java` (validation summary)
- [ ] `ExecuteImportJobRequest.java` (upload_id, import_mode)
- [ ] `ImportJobStatusResponse.java` (job details with progress)
- [ ] `ValidationErrorDTO.java` (row, field, code, message)

#### Exception/Error Codes
- [ ] Add error codes to `ErrorCode.java` enum: CRM_IMPORT_* codes
- [ ] Add translations to `messages_en.properties`, `messages_vi.properties`, etc.

#### Tests
- [ ] `CustomerImportServiceTest.java` (unit tests)
- [ ] `CustomerImportControllerTest.java` (integration tests with MockMvc)

#### Database Migration
- [ ] `V015__create_customer_import_tables.sql` (Flyway migration)

---

### Frontend Generated Files

#### Pages
- [ ] `app/crm/customers/import/page.tsx` (main import page)

#### Components
- [ ] `app/crm/customers/import/components/file-upload-form.tsx`
- [ ] `app/crm/customers/import/components/validation-summary.tsx`
- [ ] `app/crm/customers/import/components/import-job-status.tsx`

#### API Hooks
- [ ] `hooks/api/useUploadCustomerImport.ts` (TanStack Query useMutation)
- [ ] `hooks/api/useExecuteImportJob.ts` (useMutation)
- [ ] `hooks/api/useImportJobStatus.ts` (useQuery with polling)

#### Types
- [ ] `types/customer-import.types.ts` (TypeScript interfaces for DTOs)

#### Validation
- [ ] `schemas/customer-import-schema.ts` (Zod schemas for client-side validation)

#### Tests
- [ ] `components/file-upload-form.test.tsx` (React Testing Library)
- [ ] `components/import-job-status.test.tsx`

---

### Configuration Files
- [ ] Add route to `.cursor/rules/` if needed (access control)
- [ ] Update OpenAPI spec `crm-openapi-v1.yaml` with new endpoints

---

## Implementation Checklist Summary

**Total Files to Generate:** 25+ files (13 backend, 9 frontend, 3+ config/test)

**Estimated AI Code Generation Coverage:** 92%  
**Manual Developer Work Required:** 8% (deployment config, monitoring dashboard setup)

---

## Version History

| Version | Date | Changes | Author |
|---|---|---|---|
| v1.0 | 2026-03-13 | Initial feature specification for customer bulk import | BA Team |
