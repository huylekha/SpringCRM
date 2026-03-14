# ACL Service Design (Authorization & Policy Engine)

## 1. Responsibility

`acl-service` is the **RBAC source of truth** for the SpringCRM platform:

- Role and Permission management: CRUD, catalog, seeding
- User-Role assignment and revocation
- Claim management (token-level identity qualifiers)
- Internal API for `auth-service` to fetch user roles during JWT composition

> **Analogy:** Azure RBAC (`management.azure.com/roleAssignments`) + Azure AD App Roles

**Does NOT own:**
- Credentials or passwords (owned by `auth-service`)
- User profile/contact data (owned by `user-service`)
- Business permission enforcement in CRM (enforced by `crm-service` reading JWT claims)

---

## 2. Module Structure

```text
acl/
  role/
  permission/
  claim/
  assignment/   (user-role mappings)
  internal/     (API for auth-service to fetch roles)
  domain/
    entity/
    dto/
  event/
```

---

## 3. API Contracts

Base path: `/api/v1` (external, via API Gateway)
Internal base: `/internal` (private network only)

### 3.1 Role Management (External)

| Method | Path | Description | Permission | Success | Errors |
|---|---|---|---|---|---|
| POST | /roles | Create role | role:create | 201 | 400, 409 |
| GET | /roles/{id} | Get role with permissions | role:read | 200 | 404 |
| PUT | /roles/{id} | Update role metadata | role:update | 200 | 400, 404 |
| GET | /roles | List roles (paginated) | role:read | 200 | 400 |

### 3.2 Permission & Claim Management (External)

| Method | Path | Description | Permission | Success | Errors |
|---|---|---|---|---|---|
| POST | /roles/{id}/permissions | Assign permissions to role | role:assign_permission | 200 | 404, 422 |
| DELETE | /roles/{id}/permissions/{permId} | Remove permission from role | role:assign_permission | 204 | 404 |
| POST | /roles/{id}/claims | Assign claims to role | role:assign_claim | 200 | 404 |
| DELETE | /roles/{id}/claims/{claimId} | Remove claim from role | role:assign_claim | 204 | 404 |
| POST | /permissions | Create permission | permission:create | 201 | 400, 409 |
| GET | /permissions | List permissions | permission:read | 200 | 400 |
| POST | /claims | Create claim | claim:create | 201 | 409 |
| GET | /claims | List claims | claim:read | 200 | 400 |

### 3.3 User-Role Assignment (External)

| Method | Path | Description | Permission | Success | Errors |
|---|---|---|---|---|---|
| POST | /users/{userId}/roles | Assign roles to user | user:assign_role | 200 | 404, 403, 422 |
| DELETE | /users/{userId}/roles/{roleId} | Remove role from user | user:assign_role | 204 | 404 |
| GET | /users/{userId}/roles | Get user's current roles | user:read | 200 | 404 |

### 3.4 Internal Endpoints (Not exposed via Gateway)

| Method | Path | Description | Consumer |
|---|---|---|---|
| GET | /internal/users/{userId}/roles | Fetch roles+claims snapshot for JWT mint | auth-service |
| GET | /internal/roles/{roleCode}/permissions | Fetch permissions for a role | crm-service (optional cache warm) |

---

## 4. Domain Model

### Database Tables

```sql
CREATE TABLE acl_roles (
  id           CHAR(36)     PRIMARY KEY,
  role_code    VARCHAR(80)  NOT NULL UNIQUE,
  role_name    VARCHAR(120) NOT NULL,
  description  VARCHAR(300) NULL,
  is_system    BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at   DATETIME(6)  NOT NULL,
  created_by   CHAR(36)     NOT NULL,
  updated_at   DATETIME(6)  NULL,
  updated_by   CHAR(36)     NULL,
  deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
  deleted_at   DATETIME(6)  NULL,
  INDEX idx_acl_role_code (role_code, deleted)
);

CREATE TABLE acl_permissions (
  id              CHAR(36)     PRIMARY KEY,
  permission_code VARCHAR(150) NOT NULL UNIQUE,
  resource_name   VARCHAR(80)  NOT NULL,
  action_name     VARCHAR(80)  NOT NULL,
  created_at      DATETIME(6)  NOT NULL,
  created_by      CHAR(36)     NOT NULL,
  updated_at      DATETIME(6)  NULL,
  updated_by      CHAR(36)     NULL,
  deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
  INDEX idx_acl_permission_resource_action (resource_name, action_name)
);

CREATE TABLE acl_claims (
  id          CHAR(36)     PRIMARY KEY,
  claim_code  VARCHAR(120) NOT NULL UNIQUE,
  claim_name  VARCHAR(150) NOT NULL,
  created_at  DATETIME(6)  NOT NULL,
  created_by  CHAR(36)     NOT NULL,
  deleted     BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE acl_user_roles (
  id          CHAR(36)    PRIMARY KEY,
  user_id     CHAR(36)    NOT NULL,
  role_id     CHAR(36)    NOT NULL,
  assigned_at DATETIME(6) NOT NULL,
  assigned_by CHAR(36)    NOT NULL,
  UNIQUE KEY uq_user_role (user_id, role_id),
  INDEX idx_acl_user_roles_user_id (user_id),
  CONSTRAINT fk_acl_user_roles_role FOREIGN KEY (role_id) REFERENCES acl_roles(id)
);

CREATE TABLE acl_role_permissions (
  role_id       CHAR(36) NOT NULL,
  permission_id CHAR(36) NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_rp_role FOREIGN KEY (role_id) REFERENCES acl_roles(id),
  CONSTRAINT fk_rp_permission FOREIGN KEY (permission_id) REFERENCES acl_permissions(id)
);

CREATE TABLE acl_role_claims (
  role_id  CHAR(36) NOT NULL,
  claim_id CHAR(36) NOT NULL,
  PRIMARY KEY (role_id, claim_id),
  CONSTRAINT fk_rc_role FOREIGN KEY (role_id) REFERENCES acl_roles(id),
  CONSTRAINT fk_rc_claim FOREIGN KEY (claim_id) REFERENCES acl_claims(id)
);
```

---

## 5. Key Business Rules

- `SUPER_ADMIN` is a **system role** (`is_system = TRUE`) — cannot be deleted or modified via API.
- Assigning `SUPER_ADMIN` to a user requires the acting user to **already be SUPER_ADMIN** (privilege escalation guard).
- A user must have at least 1 role with `SUPER_ADMIN` in the system at all times (last-admin protection).
- Role changes take effect on the user's **next token refresh** (eventual consistency).
- Permissions are **NOT included in JWT** (too large; resolved at service level from role mapping via Redis cache).

---

## 6. Internal Role Snapshot Response (for auth-service)

`GET /internal/users/{userId}/roles`

Response:
```json
{
  "userId": "uuid",
  "roles": ["CRM_ADMIN", "AUDITOR"],
  "claims": ["crm:write", "crm:read", "audit:read"]
}
```

This endpoint is cached in Redis per `user_id` with a TTL matching the access token TTL (15 minutes). Cache is invalidated when user's roles change.

---

## 7. Events Published

| Event | Trigger | Consumer |
|---|---|---|
| `user.role.assigned` | Role assigned to user | audit-service; auth-service (invalidate role cache) |
| `user.role.revoked` | Role removed from user | audit-service; auth-service (invalidate role cache) |
| `role.permissions.changed` | Permissions on role mutated | audit-service |

---

## 8. Error Model

Common error codes:
- `ACL_ROLE_NOT_FOUND`
- `ACL_PERMISSION_NOT_FOUND`
- `ACL_CLAIM_NOT_FOUND`
- `ACL_USER_ROLE_NOT_FOUND`
- `ACL_DUPLICATE_ROLE_CODE`
- `ACL_DUPLICATE_PERMISSION_CODE`
- `ACL_PRIVILEGE_ESCALATION_DENIED`
- `ACL_LAST_ADMIN_PROTECTED`
- `ACL_SYSTEM_ROLE_PROTECTED`

---

## 9. Acceptance Criteria

- Internal role snapshot API responds within 30ms (cached path) / 100ms (DB path).
- Role assignment events published reliably for audit trail.
- SUPER_ADMIN privilege escalation guard enforced at service layer.
- Last-admin protection prevents removing all SUPER_ADMIN users.
- Permissions never included in JWT payload (resolved server-side only).
- All external endpoints enforce permission checks via `@PreAuthorize`.
