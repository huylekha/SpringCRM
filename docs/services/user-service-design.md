# User Service Design

## 1. Responsibility

`user-service` is the **Identity & Profile Manager** for the SpringCRM platform:

- User account lifecycle: create, update, deactivate, reactivate
- User profile data: fullName, email, phone, avatar, department, position
- User listing and search
- Internal profile API consumed by `auth-service` during JWT composition

> **Analogy:** Microsoft Graph API (`graph.microsoft.com/v1.0/users`)

**Does NOT own:**
- Passwords or credentials (owned by `auth-service`)
- Roles, permissions, or claims (owned by `acl-service`)

---

## 2. Module Structure

```text
user/
  controller/
  service/
  repository/
  query/
  domain/
    entity/
    dto/
  exception/
  event/
```

---

## 3. API Contracts

Base path: `/api/v1` (external, via API Gateway)
Internal base: `/internal` (private network only, not exposed externally)

### 3.1 User Management Endpoints (External)

| Method | Path | Description | Permission | Success | Errors |
|---|---|---|---|---|---|
| POST | /users | Create user account | user:create | 201 | 400, 409 |
| GET | /users/{id} | Get user profile by ID | user:read | 200 | 404 |
| PUT | /users/{id} | Update user profile | user:update | 200 | 400, 404 |
| PATCH | /users/{id}/status | Activate / Deactivate user | user:update | 200 | 404, 422 |
| GET | /users | List users (paginated) | user:read | 200 | 400 |
| POST | /users/search | Advanced user search (QueryDSL) | user:read | 200 | 400 |

### 3.2 Internal Endpoints (Not exposed via Gateway)

| Method | Path | Description | Consumer |
|---|---|---|---|
| GET | /internal/users/{userId}/profile | Fetch profile snapshot for JWT mint | auth-service |
| GET | /internal/users/{id}/exists | Verify user exists (for acl-service validation) | acl-service |

---

## 4. Domain Model

### `user_profiles` Table

```sql
CREATE TABLE user_profiles (
  id           CHAR(36)     PRIMARY KEY,
  full_name    VARCHAR(200) NOT NULL,
  email        VARCHAR(255) NOT NULL UNIQUE,
  phone        VARCHAR(50)  NULL,
  avatar_url   VARCHAR(500) NULL,
  department   VARCHAR(100) NULL,
  position     VARCHAR(100) NULL,
  status       VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
  created_at   DATETIME(6)  NOT NULL,
  created_by   CHAR(36)     NOT NULL,
  updated_at   DATETIME(6)  NULL,
  updated_by   CHAR(36)     NULL,
  deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
  deleted_at   DATETIME(6)  NULL,
  INDEX idx_user_profiles_email (email),
  INDEX idx_user_profiles_status_deleted (status, deleted),
  INDEX idx_user_profiles_created_at (created_at)
);
```

> `id` is shared with `auth-service` credential record — same UUID.
> Credential creation (by `auth-service`) always precedes profile creation (transactional saga if needed).

---

## 5. User Lifecycle Flow

### Create User (Admin)
1. Admin POSTs `/users` with {fullName, email, initialPassword, role}.
2. `user-service` validates email uniqueness.
3. `user-service` creates `user_profiles` record → generates `user_id (UUID)`.
4. `user-service` calls `auth-service` internal API: `POST /internal/credentials` with {userId, email, hashedPassword}.
5. `user-service` calls `acl-service` internal API: `POST /internal/users/{userId}/roles` with default role.
6. `user-service` publishes `user.created` event (Kafka/async) → Notification service sends welcome email.
7. Return `UserProfileResponse` (no password).

### Deactivate User
1. Admin PATCHes `/users/{id}/status` with `{status: "INACTIVE"}`.
2. `user-service` updates `user_profiles.status = INACTIVE`.
3. `user-service` publishes `user.deactivated` event.
4. `auth-service` consumes event → marks `auth_credentials.status = INACTIVE` → all active sessions are revoked.

---

## 6. Request/Response Contracts

### `POST /users` Request
```json
{
  "fullName": "Nguyen Van A",
  "email": "vana@springcrm.com",
  "phone": "0909123456",
  "department": "Sales",
  "position": "Sales Representative",
  "initialPassword": "SecureP@ss1!"
}
```

### `POST /users` Response (201)
```json
{
  "id": "uuid",
  "fullName": "Nguyen Van A",
  "email": "vana@springcrm.com",
  "phone": "0909123456",
  "department": "Sales",
  "position": "Sales Representative",
  "status": "ACTIVE",
  "createdAt": "2026-03-13T15:00:00Z",
  "createdBy": "uuid-of-admin"
}
```

### `GET /users/{id}` Response (200)
```json
{
  "id": "uuid",
  "fullName": "Nguyen Van A",
  "email": "vana@springcrm.com",
  "phone": "0909123456",
  "department": "Sales",
  "position": "Sales Representative",
  "avatarUrl": "https://cdn.springcrm.com/avatars/uuid.jpg",
  "status": "ACTIVE",
  "createdAt": "2026-03-01T10:00:00Z",
  "updatedAt": "2026-03-10T12:00:00Z"
}
```

### `GET /internal/users/{userId}/profile` Response (Internal)
```json
{
  "id": "uuid",
  "displayName": "Nguyen Van A",
  "email": "vana@springcrm.com",
  "status": "ACTIVE"
}
```

---

## 7. Error Model

```json
{
  "code": "USER_DUPLICATE_EMAIL",
  "message": "Email already registered.",
  "details": [],
  "traceId": "trace-id",
  "timestamp": "2026-03-13T15:00:00Z"
}
```

Common error codes:
- `USER_NOT_FOUND`
- `USER_DUPLICATE_EMAIL`
- `USER_INVALID_STATUS_TRANSITION`
- `USER_VALIDATION_FAILED`

---

## 8. Security

- All external endpoints require `Authorization: Bearer <JWT>`.
- Internal endpoints (`/internal/*`) are accessible only within the private network (not via API Gateway).
- Password is **never** stored or processed in `user-service`.
- `user:create` / `user:update` permissions required — enforced via `@PreAuthorize`.

---

## 9. Events Published

| Event | Trigger | Consumer |
|---|---|---|
| `user.created` | User profile created | auth-service (provision credential), notification-service |
| `user.updated` | Profile fields changed | audit-service |
| `user.deactivated` | Status → INACTIVE | auth-service (revoke sessions) |
| `user.reactivated` | Status → ACTIVE | auth-service, notification-service |

---

## 10. Acceptance Criteria

- User profile is created independently of credential and role assignment.
- Deactivation propagates to session revocation via event.
- Internal profile API returns profile snapshot within 50ms (used on every login).
- Password is never present in any response DTO from this service.
- GET list and POST search both support pagination and QueryDSL filters.
