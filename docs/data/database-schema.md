# Database Schema (MySQL)

## 1. Database Strategy

- Primary database engine: MySQL 8+ (InnoDB).
- ID strategy: UUID (stored as `CHAR(36)` for readability; optional `BINARY(16)` optimization in later phase).
- Audit and soft-delete columns are mandatory on domain tables.
- Timezone standard: UTC.

## 2. Shared Column Standards

Each business table includes:

- `id` (UUID PK)
- `created_at` `DATETIME(6)` not null
- `created_by` `VARCHAR(36)` not null
- `updated_at` `DATETIME(6)` null
- `updated_by` `VARCHAR(36)` null
- `deleted` `BOOLEAN` not null default false
- `deleted_at` `DATETIME(6)` null

## 3. IAM Service Schemas (3 Separate Databases)

Following the **Database per Service** pattern, each IAM service owns an independent database.
No cross-service FOREIGN KEY constraints exist between these databases.
Cross-service references use `user_id` (UUID) as a **logical soft reference**.

### 3.1 Auth Service DB — `springcrm_auth`

Owns only credential and session data. Profile and RBAC data are owned by their respective services.

```sql
CREATE TABLE auth_credentials (
  id              CHAR(36)     PRIMARY KEY,
  username        VARCHAR(100) NOT NULL UNIQUE,
  password_hash   VARCHAR(255) NOT NULL,
  status          VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
  failed_attempts INT          NOT NULL DEFAULT 0,
  locked_until    DATETIME(6)  NULL,
  last_login_at   DATETIME(6)  NULL,
  created_at      DATETIME(6)  NOT NULL,
  updated_at      DATETIME(6)  NULL,
  INDEX idx_auth_cred_username (username),
  INDEX idx_auth_cred_status (status)
);

CREATE TABLE auth_sessions (
  id                 CHAR(36)     PRIMARY KEY,
  user_id            CHAR(36)     NOT NULL,
  refresh_token_hash VARCHAR(255) NOT NULL UNIQUE,
  device_info        VARCHAR(255) NULL,
  ip_address         VARCHAR(45)  NULL,
  expires_at         DATETIME(6)  NOT NULL,
  is_revoked         BOOLEAN      NOT NULL DEFAULT FALSE,
  created_at         DATETIME(6)  NOT NULL,
  INDEX idx_sessions_user_id (user_id),
  INDEX idx_sessions_token_hash (refresh_token_hash),
  INDEX idx_sessions_expires_revoked (expires_at, is_revoked)
);
```

### 3.2 User Service DB — `springcrm_user`

Owns all user biographical and contact profile data.

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

### 3.3 ACL Service DB — `springcrm_acl`

Owns all RBAC definitions and user-role assignments.

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
  INDEX idx_acl_role_code (role_code)
);

CREATE TABLE acl_permissions (
  id              CHAR(36)     PRIMARY KEY,
  permission_code VARCHAR(150) NOT NULL UNIQUE,
  resource_name   VARCHAR(80)  NOT NULL,
  action_name     VARCHAR(80)  NOT NULL,
  created_at      DATETIME(6)  NOT NULL,
  created_by      CHAR(36)     NOT NULL,
  deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
  INDEX idx_acl_perm_resource_action (resource_name, action_name)
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
  CONSTRAINT fk_rp_perm FOREIGN KEY (permission_id) REFERENCES acl_permissions(id)
);

CREATE TABLE acl_role_claims (
  role_id  CHAR(36) NOT NULL,
  claim_id CHAR(36) NOT NULL,
  PRIMARY KEY (role_id, claim_id),
  CONSTRAINT fk_rc_role FOREIGN KEY (role_id) REFERENCES acl_roles(id),
  CONSTRAINT fk_rc_claim FOREIGN KEY (claim_id) REFERENCES acl_claims(id)
);
```


## 4. CRM Schema

### 4.1 Core Aggregates

- `crm_customer`
- `crm_lead`
- `crm_opportunity`
- `crm_activity`
- `crm_task`
- `crm_note`

### 4.2 DDL Reference (CRM Core)

```sql
CREATE TABLE crm_customer (
  id CHAR(36) PRIMARY KEY,
  customer_code VARCHAR(60) NOT NULL UNIQUE,
  full_name VARCHAR(200) NOT NULL,
  email VARCHAR(255) NULL,
  phone VARCHAR(50) NULL,
  company_name VARCHAR(200) NULL,
  owner_user_id CHAR(36) NOT NULL,
  status VARCHAR(40) NOT NULL,
  source VARCHAR(60) NULL,
  created_at DATETIME(6) NOT NULL,
  created_by VARCHAR(36) NOT NULL,
  updated_at DATETIME(6) NULL,
  updated_by VARCHAR(36) NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at DATETIME(6) NULL,
  INDEX idx_customer_owner_status_deleted (owner_user_id, status, deleted),
  INDEX idx_customer_name_deleted (full_name, deleted),
  INDEX idx_customer_created_at (created_at)
);

CREATE TABLE crm_lead (
  id CHAR(36) PRIMARY KEY,
  lead_code VARCHAR(60) NOT NULL UNIQUE,
  customer_id CHAR(36) NULL,
  title VARCHAR(200) NOT NULL,
  contact_name VARCHAR(150) NULL,
  contact_email VARCHAR(255) NULL,
  contact_phone VARCHAR(50) NULL,
  owner_user_id CHAR(36) NOT NULL,
  status VARCHAR(40) NOT NULL,
  priority VARCHAR(30) NOT NULL,
  expected_value DECIMAL(18,2) NULL,
  expected_close_date DATE NULL,
  created_at DATETIME(6) NOT NULL,
  created_by VARCHAR(36) NOT NULL,
  updated_at DATETIME(6) NULL,
  updated_by VARCHAR(36) NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at DATETIME(6) NULL,
  CONSTRAINT fk_lead_customer FOREIGN KEY (customer_id) REFERENCES crm_customer(id),
  INDEX idx_lead_owner_status_deleted (owner_user_id, status, deleted),
  INDEX idx_lead_expected_close_date (expected_close_date),
  INDEX idx_lead_created_at (created_at)
);
```

## 5. Index Strategy by Access Pattern

- Authentication:
  - unique indexes on `username`, `email`, token hash.
  - `(user_id, revoked, expires_at)` for refresh token lookup.
- CRM list/search:
  - owner/status/deleted composite indexes.
  - date-range indexes for timeline and due-date queries.
  - code uniqueness per aggregate for stable external references.

## 6. Search and Pagination Performance Rules

- All list/search queries must include `deleted = false`.
- Default sort by `created_at DESC` unless client provides whitelisted fields.
- Maximum page size hard cap: `100`.
- Query plan review required for any new dynamic filter field.

## 7. Data Integrity Rules

- Enforce foreign key constraints for ownership and parent-child relationships.
- Prevent hard delete in application layer for CRM/auth business entities.
- Apply optimistic locking where concurrent update conflicts are possible.

## 8. Migration and Seed Policy

- Migration tooling: Flyway (per-service, per-database).
- Each service has its own Flyway migration path:

  **Auth Service (`springcrm_auth`):**
  - `V1__init_auth_credentials.sql`
  - `V2__init_auth_sessions.sql`

  **User Service (`springcrm_user`):**
  - `V1__init_user_profiles.sql`
  - `V2__seed_system_admin_profile.sql`

  **ACL Service (`springcrm_acl`):**
  - `V1__init_acl_schema.sql`
  - `V2__seed_roles.sql`
  - `V3__seed_permissions.sql`
  - `V4__seed_role_permissions.sql`

  **CRM Service (`springcrm_crm`):**
  - `V1__init_crm_schema.sql`
  - `V2__seed_crm_defaults.sql` (optional)

- Seed includes baseline roles and permissions only, no business data.
- Schema changes across services must be **backward compatible** — additive first.

