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

## 3. Auth Schema

### 3.1 Core Tables

- `auth_user`
  - identity and profile metadata.
- `auth_role`
  - role definition (`ADMIN`, `MANAGER`, `SALES_REP`).
- `auth_claim`
  - normalized claim dictionary.
- `auth_permission`
  - resource-action permission (`customer:read`, `lead:update`).
- `auth_user_role`
  - user-role mapping.
- `auth_role_claim`
  - role-claim mapping.
- `auth_role_permission`
  - role-permission mapping.
- `auth_refresh_token`
  - rotation-safe refresh token metadata and revocation state.

### 3.2 DDL Reference (Auth)

```sql
CREATE TABLE auth_user (
  id CHAR(36) PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  email VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(30) NOT NULL,
  last_login_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL,
  created_by VARCHAR(36) NOT NULL,
  updated_at DATETIME(6) NULL,
  updated_by VARCHAR(36) NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at DATETIME(6) NULL,
  INDEX idx_auth_user_status_deleted (status, deleted),
  INDEX idx_auth_user_created_at (created_at)
);

CREATE TABLE auth_role (
  id CHAR(36) PRIMARY KEY,
  role_code VARCHAR(80) NOT NULL UNIQUE,
  role_name VARCHAR(120) NOT NULL,
  description VARCHAR(300) NULL,
  created_at DATETIME(6) NOT NULL,
  created_by VARCHAR(36) NOT NULL,
  updated_at DATETIME(6) NULL,
  updated_by VARCHAR(36) NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at DATETIME(6) NULL,
  INDEX idx_auth_role_deleted (deleted)
);

CREATE TABLE auth_claim (
  id CHAR(36) PRIMARY KEY,
  claim_code VARCHAR(120) NOT NULL UNIQUE,
  claim_name VARCHAR(150) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  created_by VARCHAR(36) NOT NULL,
  updated_at DATETIME(6) NULL,
  updated_by VARCHAR(36) NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at DATETIME(6) NULL
);

CREATE TABLE auth_permission (
  id CHAR(36) PRIMARY KEY,
  permission_code VARCHAR(150) NOT NULL UNIQUE,
  resource_name VARCHAR(80) NOT NULL,
  action_name VARCHAR(80) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  created_by VARCHAR(36) NOT NULL,
  updated_at DATETIME(6) NULL,
  updated_by VARCHAR(36) NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  deleted_at DATETIME(6) NULL,
  INDEX idx_auth_permission_resource_action (resource_name, action_name)
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

- Migration tooling recommendation: Flyway.
- Baseline migration:
  - `V1__init_auth_schema.sql`
  - `V2__init_crm_schema.sql`
  - `V3__seed_rbac_defaults.sql`
- Seed includes baseline roles and permissions only, no business data.
