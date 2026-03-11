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

### 3.1 Authentication Endpoints

- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`

### 3.2 User and Access Management

- `POST /users`
- `GET /users/{id}`
- `PUT /users/{id}`
- `PATCH /users/{id}/status`
- `POST /roles`
- `POST /users/{id}/roles`
- `POST /roles/{id}/claims`
- `POST /roles/{id}/permissions`

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
2. Fetch active roles/claims/permissions.
3. Issue access token + refresh token.
4. Persist refresh token metadata (`jti`, `user_id`, `expires_at`, `revoked`).
5. Return response payload with token pair and user profile snapshot.

### Refresh Flow

1. Validate refresh token signature/lookup.
2. Confirm token not revoked, not expired, and rotation version matches.
3. Recompute claims from latest role assignments.
4. Invalidate old refresh token and issue new pair.
5. Return new access token and refresh token.

## 6. Request and Response Contracts

### `POST /auth/login` Request

```json
{
  "username": "admin@example.com",
  "password": "********"
}
```

### `POST /auth/login` Response

```json
{
  "accessToken": "<jwt>",
  "refreshToken": "<opaque_or_jwt>",
  "expiresIn": 900,
  "tokenType": "Bearer",
  "user": {
    "id": "uuid",
    "username": "admin@example.com",
    "roles": ["ADMIN"],
    "claims": ["crm:read", "crm:write"]
  }
}
```

### `POST /auth/refresh` Request

```json
{
  "refreshToken": "<token>"
}
```

## 7. Security Controls

- Password hashing: BCrypt with appropriate work factor.
- Account protections:
  - failed login counter
  - temporary lockout threshold
  - suspicious login audit trail
- Sensitive data never logged (credentials, raw tokens).
- Service-level permission checks for administrative endpoints.
- CORS and trusted origin controls managed jointly with gateway policy.

## 8. Error Model

Return machine-readable errors:

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "Username or password is invalid.",
  "traceId": "trace-id",
  "timestamp": "2026-03-11T08:30:00Z"
}
```

Common auth error codes:

- `AUTH_INVALID_CREDENTIALS`
- `AUTH_ACCOUNT_LOCKED`
- `AUTH_TOKEN_EXPIRED`
- `AUTH_TOKEN_REVOKED`
- `AUTH_INSUFFICIENT_PERMISSION`

## 9. Operational Requirements

- Health endpoint exposes key dependencies:
  - MySQL connectivity
  - Redis connectivity
- Security audit events generated for:
  - login success/failure
  - refresh success/failure
  - role/permission mutation
- Sentry error instrumentation enabled with trace context.

## 10. Acceptance Criteria

- Token lifecycle validates login, refresh, and logout paths.
- Role/claim changes are reflected on next token refresh.
- Unauthorized and forbidden responses are consistent and traceable.
- Administrative APIs enforce permission checks.
