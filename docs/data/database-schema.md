# Database Schema (PostgreSQL)

## 1. Database Strategy

- Primary database engine: PostgreSQL 15+.
- Database per Service: each microservice owns an isolated database.
  - `auth-service` → `springcrm_auth`
  - `crm-service` → `springcrm_crm`
- ID strategy: UUID v7 (time-ordered), stored as native PostgreSQL `UUID` type.
- Entity hierarchy: `BaseEntity<T>` → `AuditableEntity` → `SoftDeletableEntity` → `TenantEntity` → `FullAuditEntity`.
- Timezone standard: UTC (all `TIMESTAMP` columns).

## 2. Shared Column Standards

Each business table extending `FullAuditEntityUUID` includes:

| Column | Type | Constraints |
|--------|------|-------------|
| `id` | UUID | PK, auto-generated UUID v7 |
| `created_at` | TIMESTAMP | NOT NULL, immutable |
| `created_by` | UUID | auditor ID (nullable for system ops) |
| `created_by_name` | VARCHAR(200) | snapshot of auditor name at creation |
| `updated_at` | TIMESTAMP | set on every update |
| `updated_by` | UUID | last updater ID |
| `updated_by_name` | VARCHAR(200) | snapshot of updater name |
| `deleted` | BOOLEAN | NOT NULL, DEFAULT FALSE |
| `deleted_at` | TIMESTAMP | set when soft-deleted |
| `tenant_id` | VARCHAR(64) | NOT NULL, multi-tenancy key |

Audit and tenant fields are **automatically populated** by `AuditTenantEntityListener` via `RequestContext` (ThreadLocal) — no manual setting in service code.

## 3. Auth Service DB — `springcrm_auth`

### Tables

```sql
-- Core RBAC tables
auth_user             -- User credentials and profile
auth_role             -- Role definitions
auth_permission       -- Permission definitions (resource + action)
auth_claim            -- Claim definitions
auth_user_role        -- M:N user-role assignments
auth_role_permission  -- M:N role-permission assignments
auth_role_claim       -- M:N role-claim assignments
auth_refresh_token    -- JWT refresh tokens (extends BaseEntityUUID)

-- Messaging infrastructure
outbox_messages       -- Transactional outbox for domain events
inbox_messages        -- Deduplication of incoming messages
idempotency_records   -- Request idempotency tracking
```

### Key Constraints

- `auth_user.username` — UNIQUE
- `auth_user.email` — UNIQUE
- `auth_role.role_code` — UNIQUE
- `auth_permission.permission_code` — UNIQUE
- `auth_claim.claim_code` — UNIQUE
- `auth_refresh_token.token_hash` — UNIQUE

## 4. CRM Service DB — `springcrm_crm`

### Tables

```sql
-- Business tables
orders                -- Customer orders (extends FullAuditEntityUUID)
order_items           -- Order line items (extends BaseEntityUUID)

-- Messaging infrastructure
outbox_messages       -- Transactional outbox for domain events
inbox_messages        -- Deduplication of incoming messages
idempotency_records   -- Request idempotency tracking
```

### Key Constraints

- `orders.order_number` — UNIQUE
- FK: `order_items.order_id` → `orders.id`

## 5. Index Strategy

### Auth Service
- `idx_auth_user_status` — filter by active/inactive
- `idx_auth_user_tenant` — tenant isolation queries
- `idx_auth_role_tenant`, `idx_auth_permission_tenant`, `idx_auth_claim_tenant`
- `idx_auth_refresh_token_user` — lookup by user
- `idx_auth_refresh_token_expires` — cleanup expired tokens

### CRM Service
- `idx_orders_customer` — orders by customer
- `idx_orders_status` — filter by status
- `idx_orders_tenant` — tenant isolation
- `idx_orders_order_date` — date range queries
- `idx_order_items_order` — items per order

### Messaging (both services)
- `idx_outbox_messages_status` — pending message polling
- `idx_inbox_messages_event_type` — dedup lookup
- `idx_idempotency_records_expires` — cleanup expired records

## 6. Migration and Seed Policy

- Migration tooling: **Flyway** (per-service, per-database).
- Dependencies: `spring-boot-starter-flyway` + `flyway-database-postgresql`.
- Flyway runs automatically at service startup (`spring.flyway.enabled: true`).
- `baseline-on-migrate: true` — safe for new databases.
- `ddl-auto: validate` — Hibernate validates schema matches entities after Flyway runs.

### Migration Files

**Auth Service** (`backend/auth-service/src/main/resources/db/migration/`):
- `V1__init_auth_schema.sql` — all tables, constraints, indexes
- `V2__seed_rbac_data.sql` — default roles, permissions, claims, admin user

**CRM Service** (`backend/crm-service/src/main/resources/db/migration/`):
- `V1__init_crm_schema.sql` — all tables, constraints, indexes

### Naming Convention
- Flyway standard: `V{version}__{description}.sql`
- Schema-only changes: `V{N}__add_{feature}.sql`
- Data migrations: `V{N}__seed_{data}.sql`

## 7. K8s Database Configuration

Each environment uses `namePrefix` in Kustomize, which prefixes all K8s resource names.
Database hostname must match the prefixed postgres service name.

| Environment | DB_HOST | Auth DB_NAME | CRM DB_NAME |
|-------------|---------|--------------|-------------|
| Dev | `dev-postgres` | `springcrm_auth` | `springcrm_crm` |
| Staging | `staging-postgres` | `springcrm_auth` | `springcrm_crm` |
| Production | `prod-postgres` | `springcrm_auth` | `springcrm_crm` |

The postgres init script (`init-databases.sh` via ConfigMap) automatically creates the second database (`springcrm_crm`) on first startup.

## 8. Data Integrity Rules

- No hard deletes on business entities — use `softDelete()` method.
- `@SQLRestriction("deleted = false")` applied via `SoftDeletableEntity`.
- Hibernate `@Filter("tenantFilter")` enforces tenant isolation at query level.
- Foreign key constraints for parent-child relationships within a service.
- No cross-service foreign keys — use logical UUID references.
- Optimistic locking where concurrent update conflicts are possible.

## 9. Search and Pagination

- All queries filter `deleted = false` automatically via `@SQLRestriction`.
- Default sort: `created_at DESC`.
- Maximum page size hard cap: `100`.
