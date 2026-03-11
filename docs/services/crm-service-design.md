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

### Opportunity

- `DISCOVERY` -> `VALIDATION` -> `NEGOTIATION` -> `WON` or `LOST`

### Task

- `TODO` -> `IN_PROGRESS` -> `DONE` or `CANCELLED`

State changes must be validated with role-based permission checks.

## 5. API Surface (v1)

### Customer APIs

- `POST /crm/customers`
- `GET /crm/customers/{id}`
- `PUT /crm/customers/{id}`
- `PATCH /crm/customers/{id}/status`
- `POST /crm/customers/search`

### Lead APIs

- `POST /crm/leads`
- `GET /crm/leads/{id}`
- `PUT /crm/leads/{id}`
- `POST /crm/leads/{id}/qualify`
- `POST /crm/leads/{id}/convert`
- `POST /crm/leads/search`

### Opportunity APIs

- `POST /crm/opportunities`
- `GET /crm/opportunities/{id}`
- `PUT /crm/opportunities/{id}`
- `POST /crm/opportunities/{id}/stage`
- `POST /crm/opportunities/search`

### Activity APIs

- `POST /crm/activities`
- `GET /crm/activities/{id}`
- `POST /crm/activities/search`

### Task APIs

- `POST /crm/tasks`
- `GET /crm/tasks/{id}`
- `PUT /crm/tasks/{id}`
- `PATCH /crm/tasks/{id}/status`
- `POST /crm/tasks/search`

### Note APIs

- `POST /crm/notes`
- `GET /crm/notes/{id}`
- `PUT /crm/notes/{id}`
- `POST /crm/notes/search`

## 6. Validation and Business Rules

- Referential integrity:
  - `lead.customer_id` must reference active customer when present.
  - `task.assignee_user_id` must map to active auth user.
- Ownership model:
  - only owner or privileged manager role may update assigned records.
- Soft-delete:
  - delete endpoints perform logical delete only.
- Monetary fields:
  - `opportunity.amount` must be non-negative with currency policy validation.

## 7. Search and Query Strategy

- QueryDSL-based query repository per module.
- Standardized search request/response contract from search architecture doc.
- All search responses are DTO projections with pagination metadata.

## 8. Error Handling

Standardized codes include:

- `CRM_RESOURCE_NOT_FOUND`
- `CRM_INVALID_STATE_TRANSITION`
- `CRM_DUPLICATE_REFERENCE`
- `CRM_FORBIDDEN_OPERATION`
- `CRM_VALIDATION_FAILED`

Error payload shape:

```json
{
  "code": "CRM_INVALID_STATE_TRANSITION",
  "message": "Cannot move opportunity from WON to NEGOTIATION.",
  "traceId": "trace-id"
}
```

## 9. Observability and Audit

- Log key domain events:
  - lead qualified/converted
  - opportunity stage changed
  - task status changed
- Include actor user ID and trace ID in event logs.
- Capture exceptions and slow query traces to Sentry/APM stack.

## 10. Acceptance Criteria

- All module APIs are documented with request/response DTO.
- State transitions are validated and permission-protected.
- Search supports dynamic filters, sort, and pagination with predictable performance.
- Soft-delete and audit behavior are enforced across aggregates.
