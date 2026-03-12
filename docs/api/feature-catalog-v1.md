# Feature Catalog v1 - Enterprise CRM Platform

## 1. Document Purpose

This catalog defines every module capability, actor, business rule, edge case, and acceptance criteria for the CRM platform v1. It serves as the single BA-level traceability source from which API contracts, RBAC policies, and QA test bases are derived.

## 2. Platform Module Map

| Module | Service | Domain Aggregate | Primary Actor(s) |
|---|---|---|---|
| Customer | crm-service | Customer | SALES_REP, SALES_MANAGER, CRM_ADMIN |
| Lead | crm-service | Lead | SALES_REP, SALES_MANAGER, CRM_ADMIN |
| Opportunity | crm-service | Opportunity | SALES_REP, SALES_MANAGER, CRM_ADMIN |
| Activity | crm-service | Activity | SALES_REP, SALES_MANAGER |
| Task | crm-service | Task | SALES_REP, SALES_MANAGER, CRM_ADMIN |
| Note | crm-service | Note | SALES_REP, SALES_MANAGER |
| User Management | auth-service | User | CRM_ADMIN, SUPER_ADMIN |
| Role Management | auth-service | Role, Claim, Permission | SUPER_ADMIN |
| Authentication | auth-service | Token | All authenticated users |
| Gateway Policy | api-gateway | N/A (edge concern) | System-level |

## 3. Actor / Role Definitions

| Role Code | Description | Data Scope |
|---|---|---|
| SUPER_ADMIN | Full platform administration including auth and system config | All data |
| CRM_ADMIN | CRM module administration, user assignment, bulk operations | All CRM data |
| SALES_MANAGER | Team pipeline visibility, reporting, team task/opportunity management | Team data (own + assigned team) |
| SALES_REP | Individual customer/lead/opportunity/task operations | Own data only |
| AUDITOR | Read-only access to operational data and audit trails | All data (read-only) |

---

## 4. Module: Customer

### 4.1 Capabilities

| ID | Capability | Description |
|---|---|---|
| CUST-C01 | Create Customer | Register a new business contact/account with required profile fields |
| CUST-C02 | View Customer | Retrieve customer details by ID |
| CUST-C03 | Update Customer | Modify customer profile fields |
| CUST-C04 | Update Customer Status | Change customer lifecycle status (ACTIVE, INACTIVE, SUSPENDED) |
| CUST-C05 | List Customers | Paginated list with simple filters (status, owner) and sort |
| CUST-C06 | Search Customers | Advanced dynamic search with complex filter combinations |
| CUST-C07 | Soft-Delete Customer | Logically remove customer record |

### 4.2 Status Values

`ACTIVE`, `INACTIVE`, `SUSPENDED`

### 4.3 Business Rules

| Rule ID | Rule | Impact |
|---|---|---|
| CUST-R01 | `customer_code` must be unique and immutable after creation | Reject duplicate, reject update of code field |
| CUST-R02 | `email` must be valid format when provided | Validation error on create/update |
| CUST-R03 | `owner_user_id` must reference an active auth_user | Referential integrity check |
| CUST-R04 | Only owner, SALES_MANAGER of owner's team, or CRM_ADMIN+ may update | Authorization enforcement |
| CUST-R05 | Status transition: ACTIVE <-> INACTIVE, ACTIVE -> SUSPENDED, SUSPENDED -> INACTIVE | Invalid transitions return 422 |
| CUST-R06 | Soft-deleted customer cannot be updated or referenced by new leads | Filter and constraint enforcement |
| CUST-R07 | `full_name` is required and max 200 characters | Validation on create/update |

### 4.4 Edge Cases

- Attempt to create customer with duplicate `customer_code` -> 409 Conflict.
- Attempt to assign `owner_user_id` to a deleted/inactive user -> 422.
- Attempt to soft-delete a customer with active leads -> warn or block per policy.
- Search with unsupported filter field -> 400 with `SEARCH_INVALID_FILTER_FIELD`.

### 4.5 Acceptance Criteria

- AC-CUST-01: Customer created with valid payload returns 201 with UUID and customer_code.
- AC-CUST-02: GET by valid ID returns full customer DTO; GET by non-existent ID returns 404.
- AC-CUST-03: PUT with valid payload updates fields and returns 200.
- AC-CUST-04: PATCH status with invalid transition returns 422 with `CRM_INVALID_STATE_TRANSITION`.
- AC-CUST-05: GET list returns paginated results respecting RBAC data scope.
- AC-CUST-06: POST search with valid filters returns matching results with pagination metadata.
- AC-CUST-07: Soft-delete sets `deleted=true`, `deleted_at=now`; subsequent GET returns 404.

---

## 5. Module: Lead

### 5.1 Capabilities

| ID | Capability | Description |
|---|---|---|
| LEAD-C01 | Create Lead | Register a new sales lead with contact and business details |
| LEAD-C02 | View Lead | Retrieve lead details by ID |
| LEAD-C03 | Update Lead | Modify lead fields (title, contact, priority, expected values) |
| LEAD-C04 | Qualify Lead | Transition lead status from NEW to QUALIFIED |
| LEAD-C05 | Convert Lead | Convert QUALIFIED/PROPOSAL lead into customer + opportunity |
| LEAD-C06 | List Leads | Paginated list with simple filters |
| LEAD-C07 | Search Leads | Advanced search with complex filter combinations |
| LEAD-C08 | Soft-Delete Lead | Logically remove lead record |

### 5.2 Lifecycle States

`NEW` -> `QUALIFIED` -> `PROPOSAL` -> `CONVERTED` | `LOST`

### 5.3 Business Rules

| Rule ID | Rule | Impact |
|---|---|---|
| LEAD-R01 | `lead_code` must be unique and immutable after creation | Reject duplicate |
| LEAD-R02 | `customer_id`, when present, must reference an active, non-deleted customer | Referential integrity |
| LEAD-R03 | State transition must follow lifecycle: NEW->QUALIFIED->PROPOSAL->CONVERTED/LOST | Invalid transition returns 422 |
| LEAD-R04 | Qualify action: only valid from NEW status | Precondition check |
| LEAD-R05 | Convert action: only valid from QUALIFIED or PROPOSAL status | Precondition check |
| LEAD-R06 | Convert creates a new Customer (if not linked) and an Opportunity atomically | Transaction boundary |
| LEAD-R07 | `expected_value` must be non-negative when provided | Validation |
| LEAD-R08 | `priority` must be one of: LOW, MEDIUM, HIGH, CRITICAL | Enum validation |
| LEAD-R09 | Only owner, team manager, or CRM_ADMIN+ may update/qualify/convert | Authorization |
| LEAD-R10 | CONVERTED or LOST leads are immutable (no further updates except notes) | Write protection |

### 5.4 Edge Cases

- Convert lead already linked to a customer -> reuse existing customer, create only opportunity.
- Convert lead with missing contact fields -> fail with validation listing required fields for customer creation.
- Attempt qualify on PROPOSAL lead -> 422 `CRM_INVALID_STATE_TRANSITION`.
- Concurrent qualify by two users -> optimistic lock handles conflict (409).

### 5.5 Acceptance Criteria

- AC-LEAD-01: Lead created with valid payload returns 201.
- AC-LEAD-02: Qualify transitions NEW lead to QUALIFIED; returns 200 with updated status.
- AC-LEAD-03: Convert on QUALIFIED lead creates customer+opportunity; returns 200 with conversion result containing new IDs.
- AC-LEAD-04: Invalid state transition returns 422 with `CRM_INVALID_STATE_TRANSITION` and descriptive message.
- AC-LEAD-05: CONVERTED lead rejects further PUT updates with 422.
- AC-LEAD-06: Search by `priority=HIGH AND status=NEW` returns correct filtered results.

---

## 6. Module: Opportunity

### 6.1 Capabilities

| ID | Capability | Description |
|---|---|---|
| OPP-C01 | Create Opportunity | Register a new revenue pipeline item |
| OPP-C02 | View Opportunity | Retrieve opportunity details by ID |
| OPP-C03 | Update Opportunity | Modify opportunity fields |
| OPP-C04 | Change Stage | Advance or regress opportunity stage |
| OPP-C05 | List Opportunities | Paginated list with simple filters |
| OPP-C06 | Search Opportunities | Advanced search with complex filter combinations |
| OPP-C07 | Soft-Delete Opportunity | Logically remove opportunity |

### 6.2 Pipeline Stages

`DISCOVERY` -> `VALIDATION` -> `NEGOTIATION` -> `WON` | `LOST`

### 6.3 Business Rules

| Rule ID | Rule | Impact |
|---|---|---|
| OPP-R01 | `amount` must be non-negative with max 2 decimal places | Validation |
| OPP-R02 | `currency` must follow ISO 4217 standard (default: USD if not specified) | Enum/format validation |
| OPP-R03 | Stage transitions follow pipeline: DISCOVERY->VALIDATION->NEGOTIATION->WON/LOST | Invalid transition returns 422 |
| OPP-R04 | Backward transition allowed: NEGOTIATION->VALIDATION, VALIDATION->DISCOVERY | Business flexibility |
| OPP-R05 | WON or LOST stages are terminal; no further stage changes | Write protection |
| OPP-R06 | `close_date` required when entering NEGOTIATION or later stages | Conditional validation |
| OPP-R07 | Only owner, team manager, or CRM_ADMIN+ may modify | Authorization |
| OPP-R08 | `customer_id` must reference active customer when present | Referential integrity |

### 6.4 Edge Cases

- Attempt stage change from DISCOVERY directly to WON -> 422 (must pass through intermediate stages).
- Update amount on WON opportunity -> allowed only by CRM_ADMIN for post-close corrections.
- Search by `stage=NEGOTIATION AND amount GTE 100000` -> returns filtered paginated results.

### 6.5 Acceptance Criteria

- AC-OPP-01: Opportunity created returns 201 with UUID.
- AC-OPP-02: Stage change DISCOVERY->VALIDATION returns 200 with updated stage.
- AC-OPP-03: Stage change DISCOVERY->WON returns 422.
- AC-OPP-04: WON opportunity rejects further stage changes with 422.
- AC-OPP-05: List returns paginated opportunities scoped by RBAC.

---

## 7. Module: Activity

### 7.1 Capabilities

| ID | Capability | Description |
|---|---|---|
| ACT-C01 | Create Activity | Log an interaction (call, meeting, email, demo) |
| ACT-C02 | View Activity | Retrieve activity details by ID |
| ACT-C03 | List Activities | Paginated list with simple filters |
| ACT-C04 | Search Activities | Advanced search with complex filter combinations |

### 7.2 Activity Types

`CALL`, `MEETING`, `EMAIL`, `DEMO`

### 7.3 Business Rules

| Rule ID | Rule | Impact |
|---|---|---|
| ACT-R01 | `type` must be one of the defined activity types | Enum validation |
| ACT-R02 | Must be linked to at least one entity (customer_id, lead_id, or opportunity_id) | Referential rule |
| ACT-R03 | Referenced entity must exist and not be soft-deleted | Integrity check |
| ACT-R04 | Activities are append-only; no update or delete after creation | Immutability policy |
| ACT-R05 | `activity_date` must not be in the future (log of past/present interaction) | Temporal validation |

### 7.4 Edge Cases

- Create activity referencing a soft-deleted lead -> 422.
- Create activity without any entity link -> 422 with descriptive validation error.
- Search activities by `type=CALL AND activity_date BETWEEN 2026-01-01 AND 2026-03-31` -> returns filtered results.

### 7.5 Acceptance Criteria

- AC-ACT-01: Activity created with valid entity link returns 201.
- AC-ACT-02: Activity referencing non-existent entity returns 404/422.
- AC-ACT-03: GET list filtered by type and date returns correct results.
- AC-ACT-04: PUT/DELETE on activity returns 405 Method Not Allowed.

---

## 8. Module: Task

### 8.1 Capabilities

| ID | Capability | Description |
|---|---|---|
| TASK-C01 | Create Task | Create an action item with assignee, due date, priority |
| TASK-C02 | View Task | Retrieve task details by ID |
| TASK-C03 | Update Task | Modify task fields (title, description, due date, priority) |
| TASK-C04 | Update Task Status | Change task lifecycle status |
| TASK-C05 | List Tasks | Paginated list with simple filters |
| TASK-C06 | Search Tasks | Advanced search with complex filter combinations |
| TASK-C07 | Soft-Delete Task | Logically remove task |

### 8.2 Lifecycle States

`TODO` -> `IN_PROGRESS` -> `DONE` | `CANCELLED`

### 8.3 Business Rules

| Rule ID | Rule | Impact |
|---|---|---|
| TASK-R01 | `assignee_user_id` must map to an active auth_user | Referential integrity |
| TASK-R02 | Status transition: TODO->IN_PROGRESS->DONE/CANCELLED; TODO->CANCELLED also valid | Lifecycle enforcement |
| TASK-R03 | DONE or CANCELLED tasks cannot be updated (except by CRM_ADMIN for corrections) | Write protection |
| TASK-R04 | `due_date` must be today or in the future at creation time | Temporal validation |
| TASK-R05 | `priority` must be one of: LOW, MEDIUM, HIGH, CRITICAL | Enum validation |
| TASK-R06 | May be linked to customer_id, lead_id, or opportunity_id (optional) | Reference association |
| TASK-R07 | Only assignee, creator, team manager, or CRM_ADMIN+ may update | Authorization |

### 8.4 Edge Cases

- Assign task to inactive user -> 422.
- Attempt IN_PROGRESS -> TODO (backward) -> 422.
- Overdue task (past due_date) can still be moved to DONE.
- Search tasks by `assignee_user_id=X AND status=TODO AND due_date LTE 2026-03-15` (overdue tasks).

### 8.5 Acceptance Criteria

- AC-TASK-01: Task created returns 201 with UUID.
- AC-TASK-02: PATCH status TODO->IN_PROGRESS returns 200.
- AC-TASK-03: PATCH status DONE->IN_PROGRESS returns 422.
- AC-TASK-04: List tasks for a given assignee respects RBAC scope.
- AC-TASK-05: Soft-deleted task excluded from list/search results.

---

## 9. Module: Note

### 9.1 Capabilities

| ID | Capability | Description |
|---|---|---|
| NOTE-C01 | Create Note | Add a contextual annotation to a CRM entity |
| NOTE-C02 | View Note | Retrieve note by ID |
| NOTE-C03 | Update Note | Modify note content |
| NOTE-C04 | List Notes | Paginated list filtered by linked entity |
| NOTE-C05 | Search Notes | Advanced search with complex filter combinations |
| NOTE-C06 | Soft-Delete Note | Logically remove note |

### 9.2 Business Rules

| Rule ID | Rule | Impact |
|---|---|---|
| NOTE-R01 | Must be linked to at least one entity (customer_id, lead_id, or opportunity_id) | Referential rule |
| NOTE-R02 | Referenced entity must exist and not be soft-deleted | Integrity check |
| NOTE-R03 | `content` is required and max 5000 characters | Validation |
| NOTE-R04 | Only creator, entity owner, or CRM_ADMIN+ may update/delete | Authorization |

### 9.3 Edge Cases

- Create note with empty content -> 422 validation error.
- Create note referencing soft-deleted entity -> 422.
- Update note by user who is neither creator nor entity owner -> 403.

### 9.4 Acceptance Criteria

- AC-NOTE-01: Note created with valid entity link returns 201.
- AC-NOTE-02: Update note content returns 200 with updated body.
- AC-NOTE-03: List notes for a customer returns paginated results ordered by created_at DESC.
- AC-NOTE-04: Soft-delete note excludes it from subsequent queries.

---

## 10. Module: User Management (Auth Service)

### 10.1 Capabilities

| ID | Capability | Description |
|---|---|---|
| USER-C01 | Create User | Register a new platform user with credentials and profile |
| USER-C02 | View User | Retrieve user profile by ID |
| USER-C03 | Update User | Modify user profile fields (not password) |
| USER-C04 | Update User Status | Activate, deactivate, or lock user account |
| USER-C05 | Assign Roles | Map roles to a user |
| USER-C06 | Remove Roles | Unmap roles from a user |
| USER-C07 | List Users | Paginated user list with filters |
| USER-C08 | Search Users | Advanced search on user attributes |
| USER-C09 | Reset Password | Admin-initiated password reset (future: self-service) |

### 10.2 User Status Values

`ACTIVE`, `INACTIVE`, `LOCKED`

### 10.3 Business Rules

| Rule ID | Rule | Impact |
|---|---|---|
| USER-R01 | `username` must be unique (case-insensitive) | Unique constraint |
| USER-R02 | `email` must be unique and valid format | Unique constraint + format |
| USER-R03 | Password must meet complexity policy (min 8 chars, mixed case, digit, special) | Validation |
| USER-R04 | Only SUPER_ADMIN or CRM_ADMIN may create/modify users | Authorization |
| USER-R05 | SUPER_ADMIN role assignment requires SUPER_ADMIN actor | Privilege escalation prevention |
| USER-R06 | Deactivating a user invalidates all active refresh tokens | Cascading token revocation |
| USER-R07 | LOCKED status set automatically after N failed login attempts | Security automation |
| USER-R08 | Cannot delete the last SUPER_ADMIN user | Safety guard |

### 10.4 Edge Cases

- Create user with duplicate email -> 409 Conflict.
- Assign SUPER_ADMIN role by CRM_ADMIN -> 403 Forbidden.
- Lock last SUPER_ADMIN -> allowed (automated), but manual deactivation blocked.
- Deactivate user who owns CRM records -> warn, records remain but ownership transfer recommended.

### 10.5 Acceptance Criteria

- AC-USER-01: User created with valid payload returns 201 (password never in response).
- AC-USER-02: GET user returns profile DTO without password_hash.
- AC-USER-03: Role assignment reflected in next JWT refresh.
- AC-USER-04: Deactivated user cannot login; returns `AUTH_ACCOUNT_LOCKED`.
- AC-USER-05: List users returns paginated results filtered by status.

---

## 11. Module: Role / Claim / Permission Management (Auth Service)

### 11.1 Capabilities

| ID | Capability | Description |
|---|---|---|
| ROLE-C01 | Create Role | Define a new role with code and description |
| ROLE-C02 | View Role | Retrieve role with associated claims and permissions |
| ROLE-C03 | Update Role | Modify role metadata |
| ROLE-C04 | List Roles | Paginated role list |
| ROLE-C05 | Assign Claims to Role | Map claims to a role |
| ROLE-C06 | Assign Permissions to Role | Map permissions to a role |
| ROLE-C07 | Remove Claims from Role | Unmap claims from a role |
| ROLE-C08 | Remove Permissions from Role | Unmap permissions from a role |
| ROLE-C09 | Create Claim | Define a new token-level claim |
| ROLE-C10 | Create Permission | Define a new resource:action permission |
| ROLE-C11 | List Claims | Paginated claim list |
| ROLE-C12 | List Permissions | Paginated permission list |

### 11.2 Business Rules

| Rule ID | Rule | Impact |
|---|---|---|
| ROLE-R01 | `role_code` must be unique and uppercase | Uniqueness + naming convention |
| ROLE-R02 | `permission_code` follows `resource:action` format | Format validation |
| ROLE-R03 | Only SUPER_ADMIN may create/modify roles, claims, permissions | Authorization |
| ROLE-R04 | Cannot delete a role that is assigned to active users | Referential safety |
| ROLE-R05 | Seed roles (SUPER_ADMIN, CRM_ADMIN, SALES_MANAGER, SALES_REP, AUDITOR) cannot be deleted | System integrity |
| ROLE-R06 | Claim/permission changes take effect on next JWT refresh | Eventual consistency |

### 11.3 Acceptance Criteria

- AC-ROLE-01: Role created returns 201 with UUID.
- AC-ROLE-02: Permission assigned to role returns 200; reflected in role detail.
- AC-ROLE-03: Attempt to delete seed role returns 422 with descriptive error.
- AC-ROLE-04: List permissions returns paginated results.

---

## 12. Module: Authentication (Auth Service)

### 12.1 Capabilities

| ID | Capability | Description |
|---|---|---|
| AUTH-C01 | Login | Authenticate with credentials, receive token pair |
| AUTH-C02 | Refresh Token | Rotate refresh token, receive new token pair |
| AUTH-C03 | Logout | Revoke current refresh token |

### 12.2 Business Rules

| Rule ID | Rule | Impact |
|---|---|---|
| AUTH-R01 | Login requires valid username/email + password | Credential validation |
| AUTH-R02 | Failed login increments counter; lock after threshold (e.g. 5 attempts) | Security |
| AUTH-R03 | Refresh token must not be revoked, expired, or version-mismatched | Token validation |
| AUTH-R04 | Refresh rotation: old token invalidated, new pair issued | Rotation security |
| AUTH-R05 | Logout revokes specific refresh token by jti | Token revocation |
| AUTH-R06 | Access token TTL: 15 minutes; Refresh token TTL: 7-30 days (configurable) | Token lifecycle |
| AUTH-R07 | Credentials and raw tokens must never appear in logs | Security compliance |

### 12.3 Edge Cases

- Login with locked account -> `AUTH_ACCOUNT_LOCKED`.
- Refresh with already-revoked token -> `AUTH_TOKEN_REVOKED` (potential token theft detection).
- Logout with invalid/expired token -> still returns 200 (idempotent).
- Concurrent refresh attempts with same token -> first succeeds, second fails with `AUTH_TOKEN_REVOKED`.

### 12.4 Acceptance Criteria

- AC-AUTH-01: Valid login returns 200 with access_token, refresh_token, expiresIn, user profile.
- AC-AUTH-02: Invalid credentials return 401 with `AUTH_INVALID_CREDENTIALS`.
- AC-AUTH-03: Refresh with valid token returns new pair; old refresh token is invalidated.
- AC-AUTH-04: Logout returns 200; subsequent refresh with same token fails.
- AC-AUTH-05: Locked account returns 401 with `AUTH_ACCOUNT_LOCKED`.

---

## 13. Module: Gateway Policy (API Gateway)

### 13.1 Capabilities

| ID | Capability | Description |
|---|---|---|
| GW-C01 | Route Forwarding | Route requests to correct downstream service |
| GW-C02 | Token Pre-validation | Validate JWT presence, format, expiry at edge |
| GW-C03 | Rate Limiting | Throttle requests per user/IP with Redis counters |
| GW-C04 | Correlation Propagation | Generate and forward X-Correlation-Id |
| GW-C05 | Request Logging | Log method, path, status, latency, user ID |
| GW-C06 | Circuit Breaking | Open circuit on repeated downstream failures |
| GW-C07 | Health Aggregation | Expose composite health of downstream services |

### 13.2 Policies

| Rule ID | Rule | Impact |
|---|---|---|
| GW-R01 | Public routes: `/auth/login`, `/auth/refresh`, health endpoints | No token required |
| GW-R02 | All other routes require valid Bearer token | 401 if missing/invalid |
| GW-R03 | Rate limit: login endpoint stricter (e.g. 10/min) than CRM reads (e.g. 100/min) | Differentiated throttling |
| GW-R04 | Timeout: auth routes 5s, CRM search 10s, CRM write 5s | Route-specific timeout |
| GW-R05 | Retry only on idempotent GET, max 2 attempts | Safe retry policy |
| GW-R06 | Circuit breaker opens after 5 consecutive 5xx from downstream | Resilience |

### 13.3 Acceptance Criteria

- AC-GW-01: Request to `/crm/customers` without token returns 401.
- AC-GW-02: Request exceeding rate limit returns 429 with retry metadata.
- AC-GW-03: X-Correlation-Id present in response headers.
- AC-GW-04: Downstream timeout returns 504 with `GATEWAY_DOWNSTREAM_TIMEOUT`.
- AC-GW-05: Health endpoint returns aggregated status.

---

## 14. Endpoint Traceability Matrix

| Module | Endpoint | Permission Required | Success Code | Error Codes |
|---|---|---|---|---|
| Customer | POST /crm/customers | customer:create | 201 | 400, 409, 422 |
| Customer | GET /crm/customers/{id} | customer:read | 200 | 404 |
| Customer | PUT /crm/customers/{id} | customer:update | 200 | 400, 404, 422 |
| Customer | PATCH /crm/customers/{id}/status | customer:update | 200 | 404, 422 |
| Customer | GET /crm/customers | customer:read | 200 | 400 |
| Customer | POST /crm/customers/search | customer:read | 200 | 400 |
| Customer | DELETE /crm/customers/{id} | customer:delete | 204 | 404, 422 |
| Lead | POST /crm/leads | lead:create | 201 | 400, 409, 422 |
| Lead | GET /crm/leads/{id} | lead:read | 200 | 404 |
| Lead | PUT /crm/leads/{id} | lead:update | 200 | 400, 404, 422 |
| Lead | POST /crm/leads/{id}/qualify | lead:update | 200 | 404, 422 |
| Lead | POST /crm/leads/{id}/convert | lead:convert | 200 | 404, 422 |
| Lead | GET /crm/leads | lead:read | 200 | 400 |
| Lead | POST /crm/leads/search | lead:read | 200 | 400 |
| Lead | DELETE /crm/leads/{id} | lead:delete | 204 | 404 |
| Opportunity | POST /crm/opportunities | opportunity:create | 201 | 400, 422 |
| Opportunity | GET /crm/opportunities/{id} | opportunity:read | 200 | 404 |
| Opportunity | PUT /crm/opportunities/{id} | opportunity:update | 200 | 400, 404, 422 |
| Opportunity | POST /crm/opportunities/{id}/stage | opportunity:update | 200 | 404, 422 |
| Opportunity | GET /crm/opportunities | opportunity:read | 200 | 400 |
| Opportunity | POST /crm/opportunities/search | opportunity:read | 200 | 400 |
| Opportunity | DELETE /crm/opportunities/{id} | opportunity:delete | 204 | 404 |
| Activity | POST /crm/activities | activity:create | 201 | 400, 422 |
| Activity | GET /crm/activities/{id} | activity:read | 200 | 404 |
| Activity | GET /crm/activities | activity:read | 200 | 400 |
| Activity | POST /crm/activities/search | activity:read | 200 | 400 |
| Task | POST /crm/tasks | task:create | 201 | 400, 422 |
| Task | GET /crm/tasks/{id} | task:read | 200 | 404 |
| Task | PUT /crm/tasks/{id} | task:update | 200 | 400, 404, 422 |
| Task | PATCH /crm/tasks/{id}/status | task:update | 200 | 404, 422 |
| Task | GET /crm/tasks | task:read | 200 | 400 |
| Task | POST /crm/tasks/search | task:read | 200 | 400 |
| Task | DELETE /crm/tasks/{id} | task:delete | 204 | 404 |
| Note | POST /crm/notes | note:create | 201 | 400, 422 |
| Note | GET /crm/notes/{id} | note:read | 200 | 404 |
| Note | PUT /crm/notes/{id} | note:update | 200 | 400, 404 |
| Note | GET /crm/notes | note:read | 200 | 400 |
| Note | POST /crm/notes/search | note:read | 200 | 400 |
| Note | DELETE /crm/notes/{id} | note:delete | 204 | 404 |
| Auth | POST /auth/login | (public) | 200 | 401 |
| Auth | POST /auth/refresh | (public) | 200 | 401 |
| Auth | POST /auth/logout | (authenticated) | 200 | 401 |
| User | POST /users | user:create | 201 | 400, 409 |
| User | GET /users/{id} | user:read | 200 | 404 |
| User | PUT /users/{id} | user:update | 200 | 400, 404 |
| User | PATCH /users/{id}/status | user:update | 200 | 404, 422 |
| User | GET /users | user:read | 200 | 400 |
| User | POST /users/search | user:read | 200 | 400 |
| User | POST /users/{id}/roles | user:assign_role | 200 | 404, 403, 422 |
| User | DELETE /users/{id}/roles/{roleId} | user:assign_role | 204 | 404 |
| Role | POST /roles | role:create | 201 | 400, 409 |
| Role | GET /roles/{id} | role:read | 200 | 404 |
| Role | PUT /roles/{id} | role:update | 200 | 400, 404 |
| Role | GET /roles | role:read | 200 | 400 |
| Role | POST /roles/{id}/claims | role:assign_claim | 200 | 404, 422 |
| Role | DELETE /roles/{id}/claims/{claimId} | role:assign_claim | 204 | 404 |
| Role | POST /roles/{id}/permissions | role:assign_permission | 200 | 404, 422 |
| Role | DELETE /roles/{id}/permissions/{permissionId} | role:assign_permission | 204 | 404 |
| Permission | POST /permissions | permission:create | 201 | 400, 409 |
| Permission | GET /permissions | permission:read | 200 | 400 |
| Claim | POST /claims | claim:create | 201 | 400, 409 |
| Claim | GET /claims | claim:read | 200 | 400 |

---

## 15. Cross-Cutting Concerns

### 15.1 Error Envelope Standard

All services return errors in this shape:

```json
{
  "code": "CRM_RESOURCE_NOT_FOUND",
  "message": "Customer with id 'abc-123' not found.",
  "details": [],
  "traceId": "trace-abc-123",
  "timestamp": "2026-03-11T10:00:00Z"
}
```

- `code`: machine-readable, uppercased, prefixed by domain (AUTH_, CRM_, SEARCH_, GATEWAY_).
- `message`: human-readable description.
- `details`: array of field-level validation errors (for 400/422 responses).
- `traceId`: distributed trace correlation ID.
- `timestamp`: ISO 8601 UTC.

### 15.2 Pagination Contract (GET list)

Query parameters: `page` (default 0), `size` (default 20, max 100), `sort` (e.g. `createdAt,desc`).

Response wrapper:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "sort": [{ "field": "createdAt", "direction": "DESC" }]
}
```

### 15.3 Search Contract (POST /search)

Uses the QueryDSL dynamic filter DSL as defined in search-api-architecture.md.

### 15.4 GET list vs POST search Decision Rule

| Use Case | Endpoint Style | Example |
|---|---|---|
| Simple listing with status/owner filter and pagination | GET list with query params | `GET /crm/customers?status=ACTIVE&page=0&size=20&sort=createdAt,desc` |
| Multi-field filter with operators (BETWEEN, IN, LIKE) | POST search with body | `POST /crm/customers/search` with filter DSL body |
| Frontend table with column sort + pagination | GET list | Standard table view |
| Advanced reporting / dashboard filter panels | POST search | Multi-criteria query |

### 15.5 Soft-Delete Convention

- `DELETE /resource/{id}` performs logical delete: sets `deleted=true`, `deleted_at=now()`.
- Subsequent GET/list/search excludes `deleted=true` records.
- Returns 204 No Content on success.
- No hard delete at API level.

### 15.6 Audit Fields

All mutations populate `created_by`/`updated_by` from JWT `user_id` claim. `created_at`/`updated_at` are server-side timestamps in UTC.
