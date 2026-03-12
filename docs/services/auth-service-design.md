# Auth Service Design

## 1. Responsibility

`auth-service` is the identity authority for the platform:

- user authentication
- JWT token issuing and refresh rotation
- logout and token revocation
- user lifecycle management
- RBAC source of truth (roles, claims, permissions)

## 2. Module Structure

```text
auth/
user/
role/
claim/
permission/
token/
audit/
```

## 3. Core API Contracts

Base path: `/api/v1`

### 3.1 Authentication Endpoints

| Method | Path | Description | Auth Required | Success | Errors |
|---|---|---|---|---|---|
| POST | /auth/login | Authenticate and issue token pair | No (public) | 200 | 401 |
| POST | /auth/refresh | Rotate refresh token and issue new pair | No (public) | 200 | 401 |
| POST | /auth/logout | Revoke refresh token | Yes | 200 | 401 |

### 3.2 User Management Endpoints

| Method | Path | Description | Permission | Success | Errors |
|---|---|---|---|---|---|
| POST | /users | Create user | user:create | 201 | 400, 409 |
| GET | /users/{id} | Get user by ID | user:read | 200 | 404 |
| PUT | /users/{id} | Update user profile | user:update | 200 | 400, 404, 422 |
| PATCH | /users/{id}/status | Update user status | user:update | 200 | 404, 422 |
| GET | /users | List users (paginated) | user:read | 200 | 400 |
| POST | /users/search | Advanced user search | user:read | 200 | 400 |
| POST | /users/{id}/roles | Assign roles to user | user:assign_role | 200 | 404, 403, 422 |
| DELETE | /users/{id}/roles/{roleId} | Remove role from user | user:assign_role | 204 | 404 |

### 3.3 Role Management Endpoints

| Method | Path | Description | Permission | Success | Errors |
|---|---|---|---|---|---|
| POST | /roles | Create role | role:create | 201 | 400, 409 |
| GET | /roles/{id} | Get role with claims and permissions | role:read | 200 | 404 |
| PUT | /roles/{id} | Update role metadata | role:update | 200 | 400, 404 |
| GET | /roles | List roles (paginated) | role:read | 200 | 400 |
| POST | /roles/{id}/claims | Assign claims to role | role:assign_claim | 200 | 404, 422 |
| DELETE | /roles/{id}/claims/{claimId} | Remove claim from role | role:assign_claim | 204 | 404 |
| POST | /roles/{id}/permissions | Assign permissions to role | role:assign_permission | 200 | 404, 422 |
| DELETE | /roles/{id}/permissions/{permissionId} | Remove permission from role | role:assign_permission | 204 | 404 |

### 3.4 Claim and Permission Endpoints

| Method | Path | Description | Permission | Success | Errors |
|---|---|---|---|---|---|
| POST | /claims | Create claim | claim:create | 201 | 400, 409 |
| GET | /claims | List claims (paginated) | claim:read | 200 | 400 |
| POST | /permissions | Create permission | permission:create | 201 | 400, 409 |
| GET | /permissions | List permissions (paginated) | permission:read | 200 | 400 |

## 4. Token Model

### Access Token

- JWT signed with asymmetric keypair (preferred) or secure HMAC fallback.
- Short TTL (recommended: 15 minutes).
- Contains:
  - `user_id`
  - `roles`
  - `claims`
  - `iat`, `exp`, `jti`

### Refresh Token

- Opaque random token or signed token with server-side hash storage.
- Long TTL (recommended: 7-30 days based on policy).
- Rotated on each refresh request.
- Revocation supported by:
  - token hash table + status
  - Redis blacklist/cache for quick invalidation checks.

## 5. Login and Refresh Flows

### Login Flow

1. Validate username/email + password.
2. Check account status (reject INACTIVE/LOCKED).
3. Fetch active roles/claims/permissions.
4. Issue access token + refresh token.
5. Persist refresh token metadata (`jti`, `user_id`, `expires_at`, `revoked`).
6. Reset failed login counter on success.
7. Return response payload with token pair and user profile snapshot.

### Refresh Flow

1. Validate refresh token signature/lookup.
2. Confirm token not revoked, not expired, and rotation version matches.
3. Recompute claims from latest role assignments.
4. Invalidate old refresh token and issue new pair.
5. Return new access token and refresh token.

### Logout Flow

1. Validate Bearer token.
2. Accept refresh token in request body.
3. Mark refresh token as revoked in storage.
4. Return 200 (idempotent; already-revoked tokens still return 200).

## 6. Request and Response Contracts

### `POST /auth/login` Request

```json
{
  "username": "admin@example.com",
  "password": "********"
}
```

### `POST /auth/login` Response (200)

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<opaque_or_jwt>",
  "expiresIn": 900,
  "tokenType": "Bearer",
  "user": {
    "id": "uuid",
    "username": "admin@example.com",
    "email": "admin@example.com",
    "roles": ["CRM_ADMIN"],
    "claims": ["crm:read", "crm:write"],
    "status": "ACTIVE"
  }
}
```

### `POST /auth/refresh` Request

```json
{
  "refreshToken": "<token>"
}
```

### `POST /auth/refresh` Response (200)

```json
{
  "accessToken": "<new_jwt>",
  "refreshToken": "<new_opaque_or_jwt>",
  "expiresIn": 900,
  "tokenType": "Bearer"
}
```

### `POST /auth/logout` Request

```json
{
  "refreshToken": "<token>"
}
```

### `POST /auth/logout` Response (200)

```json
{
  "message": "Logout successful."
}
```

### Create User Request

```json
{
  "username": "sales01",
  "email": "sales01@example.com",
  "password": "SecureP@ss1!",
  "fullName": "Nguyen Sales",
  "status": "ACTIVE"
}
```

### Create User Response (201)

```json
{
  "id": "uuid",
  "username": "sales01",
  "email": "sales01@example.com",
  "fullName": "Nguyen Sales",
  "status": "ACTIVE",
  "createdAt": "2026-03-11T10:00:00Z",
  "createdBy": "uuid-of-admin"
}
```

### User Detail Response (200)

```json
{
  "id": "uuid",
  "username": "sales01",
  "email": "sales01@example.com",
  "fullName": "Nguyen Sales",
  "status": "ACTIVE",
  "roles": [
    {
      "id": "uuid",
      "roleCode": "SALES_REP",
      "roleName": "Sales Representative"
    }
  ],
  "lastLoginAt": "2026-03-11T08:00:00Z",
  "createdAt": "2026-03-01T10:00:00Z",
  "createdBy": "uuid",
  "updatedAt": "2026-03-10T12:00:00Z",
  "updatedBy": "uuid"
}
```

### Update User Status Request

```json
{
  "status": "INACTIVE"
}
```

### Assign Roles to User Request

```json
{
  "roleIds": ["uuid-role-1", "uuid-role-2"]
}
```

### Assign Roles Response (200)

```json
{
  "userId": "uuid",
  "roles": [
    { "id": "uuid-role-1", "roleCode": "SALES_REP" },
    { "id": "uuid-role-2", "roleCode": "SALES_MANAGER" }
  ],
  "assignedAt": "2026-03-11T10:00:00Z",
  "assignedBy": "uuid-of-admin"
}
```

### GET Users List Query Parameters

`GET /users?status=ACTIVE&page=0&size=20&sort=createdAt,desc`

| Parameter | Type | Required | Default | Description |
|---|---|---|---|---|
| status | string | no | - | Filter by user status |
| page | int | no | 0 | Page number (0-based) |
| size | int | no | 20 | Page size (max 100) |
| sort | string | no | createdAt,desc | Sort field and direction |

### User Search Filterable Fields

`username`, `email`, `fullName`, `status`, `createdAt`

### Create Role Request

```json
{
  "roleCode": "TEAM_LEAD",
  "roleName": "Team Lead",
  "description": "Leads a sales team with pipeline visibility"
}
```

### Create Role Response (201)

```json
{
  "id": "uuid",
  "roleCode": "TEAM_LEAD",
  "roleName": "Team Lead",
  "description": "Leads a sales team with pipeline visibility",
  "createdAt": "2026-03-11T10:00:00Z",
  "createdBy": "uuid"
}
```

### Role Detail Response (200)

```json
{
  "id": "uuid",
  "roleCode": "SALES_REP",
  "roleName": "Sales Representative",
  "description": "Individual contributor for sales operations",
  "claims": [
    { "id": "uuid", "claimCode": "crm:basic" }
  ],
  "permissions": [
    { "id": "uuid", "permissionCode": "customer:read", "resourceName": "customer", "actionName": "read" },
    { "id": "uuid", "permissionCode": "customer:create", "resourceName": "customer", "actionName": "create" }
  ],
  "createdAt": "2026-03-01T10:00:00Z",
  "updatedAt": "2026-03-10T12:00:00Z"
}
```

### GET Roles List Query Parameters

`GET /roles?page=0&size=20&sort=roleCode,asc`

### Assign Claims to Role Request

```json
{
  "claimIds": ["uuid-claim-1"]
}
```

### Assign Permissions to Role Request

```json
{
  "permissionIds": ["uuid-perm-1", "uuid-perm-2"]
}
```

### Create Permission Request

```json
{
  "permissionCode": "customer:read",
  "resourceName": "customer",
  "actionName": "read"
}
```

### Create Claim Request

```json
{
  "claimCode": "crm:basic",
  "claimName": "CRM Basic Access"
}
```

### GET Permissions/Claims List Query Parameters

`GET /permissions?page=0&size=50&sort=permissionCode,asc`
`GET /claims?page=0&size=50&sort=claimCode,asc`

## 7. Security Controls

- Password hashing: BCrypt with appropriate work factor.
- Account protections:
  - failed login counter
  - temporary lockout threshold (default: 5 failed attempts)
  - suspicious login audit trail
- Sensitive data never logged (credentials, raw tokens).
- Service-level permission checks for administrative endpoints.
- CORS and trusted origin controls managed jointly with gateway policy.
- Password never returned in any response DTO.

## 8. Error Model

Return machine-readable errors:

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "Username or password is invalid.",
  "details": [],
  "traceId": "trace-id",
  "timestamp": "2026-03-11T08:30:00Z"
}
```

Common auth error codes:

- `AUTH_INVALID_CREDENTIALS`
- `AUTH_ACCOUNT_LOCKED`
- `AUTH_ACCOUNT_INACTIVE`
- `AUTH_TOKEN_EXPIRED`
- `AUTH_TOKEN_REVOKED`
- `AUTH_INSUFFICIENT_PERMISSION`
- `AUTH_DUPLICATE_USERNAME`
- `AUTH_DUPLICATE_EMAIL`
- `AUTH_ROLE_NOT_FOUND`
- `AUTH_PRIVILEGE_ESCALATION_DENIED`
- `AUTH_SEED_ROLE_PROTECTED`
- `AUTH_LAST_ADMIN_PROTECTED`
- `AUTH_VALIDATION_FAILED`
- `AUTH_PASSWORD_POLICY_VIOLATION`

## 9. Operational Requirements

- Health endpoint exposes key dependencies:
  - MySQL connectivity
  - Redis connectivity
- Security audit events generated for:
  - login success/failure
  - refresh success/failure
  - logout
  - role/permission mutation
  - user status change
  - privilege escalation attempt
- Sentry error instrumentation enabled with trace context.

## 10. Acceptance Criteria

- Token lifecycle validates login, refresh, and logout paths.
- Role/claim changes are reflected on next token refresh.
- Unauthorized and forbidden responses are consistent and traceable.
- Administrative APIs enforce permission checks.
- All endpoints documented with request/response DTO.
- GET list endpoints support pagination with query parameters.
- POST search endpoints support full QueryDSL filter DSL.
- Error responses follow the standard error envelope with `details` array.
- Password never exposed in any response.
