# CRM Service Design

## 1. Responsibility

`crm-service` owns CRM business capabilities:

- customer management
- lead lifecycle
- opportunity pipeline
- activity logging
- task management
- note management
- advanced search and reporting-ready query endpoints

## 2. Domain Modules

```text
customer/
lead/
opportunity/
activity/
task/
note/
```

## 3. Aggregate Boundaries

- `Customer`
  - Master business contact/account entity.
- `Lead`
  - Potential business demand linked or convertible to customer/opportunity.
- `Opportunity`
  - Revenue pipeline object with stage transitions.
- `Activity`
  - Logged interactions (call, meeting, email, demo).
- `Task`
  - Action items with assignee, due date, status.
- `Note`
  - Context annotations linked to customer/lead/opportunity.

## 4. Lifecycle and State Transitions

### Lead

- `NEW` -> `QUALIFIED` -> `PROPOSAL` -> `CONVERTED` or `LOST`
- Backward transitions not allowed.
- `CONVERTED` and `LOST` are terminal (immutable).

### Opportunity

- `DISCOVERY` -> `VALIDATION` -> `NEGOTIATION` -> `WON` or `LOST`
- Backward transitions allowed: NEGOTIATION->VALIDATION, VALIDATION->DISCOVERY.
- `WON` and `LOST` are terminal.

### Task

- `TODO` -> `IN_PROGRESS` -> `DONE` or `CANCELLED`
- `TODO` -> `CANCELLED` is also valid.
- `DONE` and `CANCELLED` are terminal.

State changes must be validated with role-based permission checks.

## 5. API Surface (v1)

Base path: `/api/v1`

### 5.1 Customer APIs

| Method | Path | Description | Permission | Success | Errors |
|---|---|---|---|---|---|
| POST | /crm/customers | Create customer | customer:create | 201 | 400, 409, 422 |
| GET | /crm/customers/{id} | Get customer by ID | customer:read | 200 | 404 |
| PUT | /crm/customers/{id} | Update customer | customer:update | 200 | 400, 404, 422 |
| PATCH | /crm/customers/{id}/status | Update customer status | customer:update | 200 | 404, 422 |
| DELETE | /crm/customers/{id} | Soft-delete customer | customer:delete | 204 | 404, 422 |
| GET | /crm/customers | List customers (paginated) | customer:read | 200 | 400 |
| POST | /crm/customers/search | Advanced search | customer:read | 200 | 400 |

#### Create Customer Request

```json
{
  "customerCode": "CUST-20260311-001",
  "fullName": "Nguyen Van A",
  "email": "a@example.com",
  "phone": "+84901234567",
  "companyName": "ABC Corp",
  "source": "REFERRAL",
  "status": "ACTIVE"
}
```

#### Create Customer Response (201)

```json
{
  "id": "uuid",
  "customerCode": "CUST-20260311-001",
  "fullName": "Nguyen Van A",
  "email": "a@example.com",
  "phone": "+84901234567",
  "companyName": "ABC Corp",
  "ownerUserId": "uuid-of-creator",
  "source": "REFERRAL",
  "status": "ACTIVE",
  "createdAt": "2026-03-11T10:00:00Z",
  "createdBy": "uuid-of-creator"
}
```

#### Customer Detail Response (200)

```json
{
  "id": "uuid",
  "customerCode": "CUST-20260311-001",
  "fullName": "Nguyen Van A",
  "email": "a@example.com",
  "phone": "+84901234567",
  "companyName": "ABC Corp",
  "ownerUserId": "uuid",
  "source": "REFERRAL",
  "status": "ACTIVE",
  "createdAt": "2026-03-11T10:00:00Z",
  "createdBy": "uuid",
  "updatedAt": "2026-03-11T11:00:00Z",
  "updatedBy": "uuid"
}
```

#### Update Customer Status Request

```json
{
  "status": "INACTIVE"
}
```

#### GET List Query Parameters

`GET /crm/customers?status=ACTIVE&ownerUserId=uuid&page=0&size=20&sort=createdAt,desc`

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| status | string | no | - | Filter by status |
| ownerUserId | string(uuid) | no | - | Filter by owner |
| page | int | no | 0 | Page number (0-based) |
| size | int | no | 20 | Page size (max 100) |
| sort | string | no | createdAt,desc | Sort field and direction |

#### Search Filterable Fields

`fullName`, `email`, `phone`, `status`, `ownerUserId`, `source`, `companyName`, `createdAt`

---

### 5.2 Lead APIs

| Method | Path | Description | Permission | Success | Errors |
|---|---|---|---|---|---|
| POST | /crm/leads | Create lead | lead:create | 201 | 400, 409, 422 |
| GET | /crm/leads/{id} | Get lead by ID | lead:read | 200 | 404 |
| PUT | /crm/leads/{id} | Update lead | lead:update | 200 | 400, 404, 422 |
| POST | /crm/leads/{id}/qualify | Qualify lead | lead:update | 200 | 404, 422 |
| POST | /crm/leads/{id}/convert | Convert lead | lead:convert | 200 | 404, 422 |
| DELETE | /crm/leads/{id} | Soft-delete lead | lead:delete | 204 | 404 |
| GET | /crm/leads | List leads (paginated) | lead:read | 200 | 400 |
| POST | /crm/leads/search | Advanced search | lead:read | 200 | 400 |

#### Create Lead Request

```json
{
  "leadCode": "LEAD-20260311-001",
  "customerId": null,
  "title": "New ERP Implementation",
  "contactName": "Tran Thi B",
  "contactEmail": "b@example.com",
  "contactPhone": "+84901234568",
  "priority": "HIGH",
  "expectedValue": 50000.00,
  "expectedCloseDate": "2026-06-30"
}
```

#### Create Lead Response (201)

```json
{
  "id": "uuid",
  "leadCode": "LEAD-20260311-001",
  "customerId": null,
  "title": "New ERP Implementation",
  "contactName": "Tran Thi B",
  "contactEmail": "b@example.com",
  "contactPhone": "+84901234568",
  "ownerUserId": "uuid-of-creator",
  "status": "NEW",
  "priority": "HIGH",
  "expectedValue": 50000.00,
  "expectedCloseDate": "2026-06-30",
  "createdAt": "2026-03-11T10:00:00Z",
  "createdBy": "uuid-of-creator"
}
```

#### Qualify Lead Response (200)

```json
{
  "id": "uuid",
  "status": "QUALIFIED",
  "qualifiedAt": "2026-03-11T11:00:00Z",
  "qualifiedBy": "uuid"
}
```

#### Convert Lead Request

```json
{
  "createCustomer": true,
  "customerData": {
    "fullName": "Tran Thi B",
    "email": "b@example.com",
    "phone": "+84901234568",
    "companyName": "XYZ Corp"
  },
  "opportunityData": {
    "title": "ERP Implementation Deal",
    "amount": 50000.00,
    "currency": "USD",
    "closeDate": "2026-06-30"
  }
}
```

When lead is already linked to a customer, set `createCustomer: false` and omit `customerData`.

#### Convert Lead Response (200)

```json
{
  "leadId": "uuid",
  "leadStatus": "CONVERTED",
  "customerId": "uuid-new-or-existing",
  "opportunityId": "uuid-new",
  "convertedAt": "2026-03-11T12:00:00Z",
  "convertedBy": "uuid"
}
```

#### GET List Query Parameters

`GET /crm/leads?status=NEW&priority=HIGH&ownerUserId=uuid&page=0&size=20&sort=createdAt,desc`

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| status | string | no | - | Filter by lead status |
| priority | string | no | - | Filter by priority |
| ownerUserId | string(uuid) | no | - | Filter by owner |
| page | int | no | 0 | Page number |
| size | int | no | 20 | Page size (max 100) |
| sort | string | no | createdAt,desc | Sort field and direction |

#### Search Filterable Fields

`title`, `status`, `priority`, `expectedCloseDate`, `ownerUserId`, `contactName`, `contactEmail`, `expectedValue`, `createdAt`

---

### 5.3 Opportunity APIs

| Method | Path | Description | Permission | Success | Errors |
|---|---|---|---|---|---|
| POST | /crm/opportunities | Create opportunity | opportunity:create | 201 | 400, 422 |
| GET | /crm/opportunities/{id} | Get opportunity by ID | opportunity:read | 200 | 404 |
| PUT | /crm/opportunities/{id} | Update opportunity | opportunity:update | 200 | 400, 404, 422 |
| POST | /crm/opportunities/{id}/stage | Change stage | opportunity:update | 200 | 404, 422 |
| DELETE | /crm/opportunities/{id} | Soft-delete opportunity | opportunity:delete | 204 | 404 |
| GET | /crm/opportunities | List opportunities (paginated) | opportunity:read | 200 | 400 |
| POST | /crm/opportunities/search | Advanced search | opportunity:read | 200 | 400 |

#### Create Opportunity Request

```json
{
  "customerId": "uuid",
  "leadId": "uuid-optional",
  "title": "ERP Implementation Deal",
  "amount": 50000.00,
  "currency": "USD",
  "closeDate": "2026-06-30",
  "description": "Full ERP rollout for XYZ Corp"
}
```

#### Create Opportunity Response (201)

```json
{
  "id": "uuid",
  "customerId": "uuid",
  "leadId": "uuid",
  "title": "ERP Implementation Deal",
  "ownerUserId": "uuid-of-creator",
  "stage": "DISCOVERY",
  "amount": 50000.00,
  "currency": "USD",
  "closeDate": "2026-06-30",
  "description": "Full ERP rollout for XYZ Corp",
  "createdAt": "2026-03-11T10:00:00Z",
  "createdBy": "uuid-of-creator"
}
```

#### Change Stage Request

```json
{
  "stage": "VALIDATION",
  "note": "Customer confirmed budget availability"
}
```

#### Change Stage Response (200)

```json
{
  "id": "uuid",
  "previousStage": "DISCOVERY",
  "currentStage": "VALIDATION",
  "changedAt": "2026-03-11T11:00:00Z",
  "changedBy": "uuid"
}
```

#### GET List Query Parameters

`GET /crm/opportunities?stage=DISCOVERY&ownerUserId=uuid&page=0&size=20&sort=amount,desc`

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| stage | string | no | - | Filter by stage |
| ownerUserId | string(uuid) | no | - | Filter by owner |
| customerId | string(uuid) | no | - | Filter by customer |
| page | int | no | 0 | Page number |
| size | int | no | 20 | Page size (max 100) |
| sort | string | no | createdAt,desc | Sort field and direction |

#### Search Filterable Fields

`stage`, `amount`, `currency`, `closeDate`, `ownerUserId`, `customerId`, `title`, `createdAt`

---

### 5.4 Activity APIs

| Method | Path | Description | Permission | Success | Errors |
|---|---|---|---|---|---|
| POST | /crm/activities | Create activity | activity:create | 201 | 400, 422 |
| GET | /crm/activities/{id} | Get activity by ID | activity:read | 200 | 404 |
| GET | /crm/activities | List activities (paginated) | activity:read | 200 | 400 |
| POST | /crm/activities/search | Advanced search | activity:read | 200 | 400 |

Activities are append-only; no PUT, PATCH, or DELETE endpoints.

#### Create Activity Request

```json
{
  "type": "CALL",
  "customerId": "uuid",
  "leadId": null,
  "opportunityId": null,
  "subject": "Follow-up call on pricing",
  "description": "Discussed pricing tiers and timeline",
  "activityDate": "2026-03-11T09:30:00Z",
  "durationMinutes": 30
}
```

#### Create Activity Response (201)

```json
{
  "id": "uuid",
  "type": "CALL",
  "customerId": "uuid",
  "leadId": null,
  "opportunityId": null,
  "subject": "Follow-up call on pricing",
  "description": "Discussed pricing tiers and timeline",
  "activityDate": "2026-03-11T09:30:00Z",
  "durationMinutes": 30,
  "createdAt": "2026-03-11T10:00:00Z",
  "createdBy": "uuid"
}
```

#### GET List Query Parameters

`GET /crm/activities?type=CALL&customerId=uuid&page=0&size=20&sort=activityDate,desc`

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| type | string | no | - | Filter by activity type |
| customerId | string(uuid) | no | - | Filter by customer |
| leadId | string(uuid) | no | - | Filter by lead |
| opportunityId | string(uuid) | no | - | Filter by opportunity |
| page | int | no | 0 | Page number |
| size | int | no | 20 | Page size (max 100) |
| sort | string | no | activityDate,desc | Sort field and direction |

#### Search Filterable Fields

`type`, `customerId`, `leadId`, `opportunityId`, `subject`, `activityDate`, `createdAt`

---

### 5.5 Task APIs

| Method | Path | Description | Permission | Success | Errors |
|---|---|---|---|---|---|
| POST | /crm/tasks | Create task | task:create | 201 | 400, 422 |
| GET | /crm/tasks/{id} | Get task by ID | task:read | 200 | 404 |
| PUT | /crm/tasks/{id} | Update task | task:update | 200 | 400, 404, 422 |
| PATCH | /crm/tasks/{id}/status | Update task status | task:update | 200 | 404, 422 |
| DELETE | /crm/tasks/{id} | Soft-delete task | task:delete | 204 | 404 |
| GET | /crm/tasks | List tasks (paginated) | task:read | 200 | 400 |
| POST | /crm/tasks/search | Advanced search | task:read | 200 | 400 |

#### Create Task Request

```json
{
  "title": "Send proposal to XYZ Corp",
  "description": "Prepare and send commercial proposal",
  "assigneeUserId": "uuid",
  "dueDate": "2026-03-15",
  "priority": "HIGH",
  "customerId": "uuid-optional",
  "leadId": null,
  "opportunityId": "uuid-optional"
}
```

#### Create Task Response (201)

```json
{
  "id": "uuid",
  "title": "Send proposal to XYZ Corp",
  "description": "Prepare and send commercial proposal",
  "assigneeUserId": "uuid",
  "ownerUserId": "uuid-of-creator",
  "dueDate": "2026-03-15",
  "priority": "HIGH",
  "status": "TODO",
  "customerId": "uuid",
  "leadId": null,
  "opportunityId": "uuid",
  "createdAt": "2026-03-11T10:00:00Z",
  "createdBy": "uuid"
}
```

#### Update Task Status Request

```json
{
  "status": "IN_PROGRESS"
}
```

#### Update Task Status Response (200)

```json
{
  "id": "uuid",
  "previousStatus": "TODO",
  "currentStatus": "IN_PROGRESS",
  "changedAt": "2026-03-11T11:00:00Z",
  "changedBy": "uuid"
}
```

#### GET List Query Parameters

`GET /crm/tasks?status=TODO&assigneeUserId=uuid&priority=HIGH&page=0&size=20&sort=dueDate,asc`

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| status | string | no | - | Filter by status |
| assigneeUserId | string(uuid) | no | - | Filter by assignee |
| priority | string | no | - | Filter by priority |
| customerId | string(uuid) | no | - | Filter by customer |
| page | int | no | 0 | Page number |
| size | int | no | 20 | Page size (max 100) |
| sort | string | no | dueDate,asc | Sort field and direction |

#### Search Filterable Fields

`title`, `status`, `assigneeUserId`, `ownerUserId`, `priority`, `dueDate`, `customerId`, `leadId`, `opportunityId`, `createdAt`

---

### 5.6 Note APIs

| Method | Path | Description | Permission | Success | Errors |
|---|---|---|---|---|---|
| POST | /crm/notes | Create note | note:create | 201 | 400, 422 |
| GET | /crm/notes/{id} | Get note by ID | note:read | 200 | 404 |
| PUT | /crm/notes/{id} | Update note | note:update | 200 | 400, 404 |
| DELETE | /crm/notes/{id} | Soft-delete note | note:delete | 204 | 404 |
| GET | /crm/notes | List notes (paginated) | note:read | 200 | 400 |
| POST | /crm/notes/search | Advanced search | note:read | 200 | 400 |

#### Create Note Request

```json
{
  "content": "Customer expressed interest in premium tier.",
  "customerId": "uuid",
  "leadId": null,
  "opportunityId": null
}
```

#### Create Note Response (201)

```json
{
  "id": "uuid",
  "content": "Customer expressed interest in premium tier.",
  "customerId": "uuid",
  "leadId": null,
  "opportunityId": null,
  "createdAt": "2026-03-11T10:00:00Z",
  "createdBy": "uuid"
}
```

#### GET List Query Parameters

`GET /crm/notes?customerId=uuid&page=0&size=20&sort=createdAt,desc`

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| customerId | string(uuid) | no | - | Filter by customer |
| leadId | string(uuid) | no | - | Filter by lead |
| opportunityId | string(uuid) | no | - | Filter by opportunity |
| page | int | no | 0 | Page number |
| size | int | no | 20 | Page size (max 100) |
| sort | string | no | createdAt,desc | Sort field and direction |

#### Search Filterable Fields

`content`, `customerId`, `leadId`, `opportunityId`, `createdAt`, `createdBy`

---

## 6. Validation and Business Rules

- Referential integrity:
  - `lead.customer_id` must reference active customer when present.
  - `task.assignee_user_id` must map to active auth user.
  - `activity` must link to at least one entity (customer, lead, or opportunity).
  - `note` must link to at least one entity.
- Ownership model:
  - only owner or privileged manager role may update assigned records.
- Soft-delete:
  - delete endpoints perform logical delete only.
  - soft-deleted records excluded from GET by ID (returns 404), list, and search.
- Monetary fields:
  - `opportunity.amount` must be non-negative with currency policy validation.
  - `lead.expected_value` must be non-negative.

## 7. Search and Query Strategy

- QueryDSL-based query repository per module.
- Standardized search request/response contract from search architecture doc.
- All search responses are DTO projections with pagination metadata.
- GET list endpoints use Spring Pageable with query param binding.
- POST search endpoints use the full QueryDSL filter DSL.

## 8. Error Handling

Standardized codes include:

- `CRM_RESOURCE_NOT_FOUND`
- `CRM_INVALID_STATE_TRANSITION`
- `CRM_DUPLICATE_REFERENCE`
- `CRM_FORBIDDEN_OPERATION`
- `CRM_VALIDATION_FAILED`
- `CRM_REFERENTIAL_INTEGRITY_VIOLATION`
- `CRM_IMMUTABLE_RECORD`

Error payload shape:

```json
{
  "code": "CRM_INVALID_STATE_TRANSITION",
  "message": "Cannot move opportunity from WON to NEGOTIATION.",
  "details": [],
  "traceId": "trace-id",
  "timestamp": "2026-03-11T10:00:00Z"
}
```

Validation error detail shape (for 400/422):

```json
{
  "code": "CRM_VALIDATION_FAILED",
  "message": "Request validation failed.",
  "details": [
    { "field": "fullName", "message": "must not be blank" },
    { "field": "email", "message": "must be a valid email address" }
  ],
  "traceId": "trace-id",
  "timestamp": "2026-03-11T10:00:00Z"
}
```

## 9. Observability and Audit

- Log key domain events:
  - lead qualified/converted
  - opportunity stage changed
  - task status changed
  - customer status changed
  - record soft-deleted
- Include actor user ID and trace ID in event logs.
- Capture exceptions and slow query traces to Sentry/APM stack.

## 10. Acceptance Criteria

- All module APIs are documented with request/response DTO.
- State transitions are validated and permission-protected.
- GET list supports simple filters with pagination via query params.
- POST search supports dynamic filters, sort, and pagination with predictable performance.
- Soft-delete and audit behavior are enforced across aggregates.
- All error responses follow the standard error envelope.
