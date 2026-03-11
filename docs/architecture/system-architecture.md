# System Architecture - Enterprise CRM Platform

## 1. Architecture Goals

The platform is designed to deliver a SaaS-ready CRM with:

- Strong identity and authorization controls (JWT + RBAC).
- Independent service evolution through microservice boundaries.
- High operational clarity through standardized observability and CI/CD gates.
- Query flexibility at scale for CRM search use cases.

## 2. Target System Boundaries

### Services

- `auth-service`
  - Identity, credential validation, token lifecycle, user/role/claim/permission.
- `crm-service`
  - CRM domain transaction and query operations.
- `api-gateway`
  - Edge routing and cross-cutting policy enforcement.
- `frontend` (Next.js)
  - User-facing workflows and role-aware interaction model.

### Shared Infrastructure

- `mysql` for transactional data.
- `redis` for token/session/cache/rate-limit counters.
- `sentry` + tracing backend for error/performance observability.

## 3. Architecture Style

### 3.1 Macro Architecture

- Style: microservices + gateway + frontend BFF-like interaction through centralized edge.
- Communication:
  - Sync: HTTP/JSON for user-facing request-response paths.
  - Async-ready extension point: event publishing for audit/integration workflows (future phase).

### 3.2 Service Internal Architecture

Each backend service follows clean layering:

1. `controller` (transport mapping, validation, response envelope).
2. `service` (business orchestration and policy enforcement).
3. `repository` and `query repository` (command/query separation).
4. QueryDSL projection to DTO.
5. persistence model (`entity`) isolated from API contract.

## 4. Non-Functional Requirements (NFR)

### Security

- JWT access token with short TTL, refresh token rotation.
- RBAC authorization with claims and permission mapping.
- Defense in depth:
  - Gateway pre-validation.
  - Service-level authorization checks for sensitive operations.
- Standardized security audit events for login/token/permission changes.

### Scalability and Performance

- Stateless backend services (horizontal scaling friendly).
- Pageable API conventions for list/search endpoints.
- QueryDSL dynamic filtering with indexed columns only for high-cardinality conditions.
- Optional read-cache path for frequent lookup entities.

### Reliability and Availability

- Timeouts/retries/circuit-breaker strategy at gateway and outbound client boundaries.
- Graceful degradation for non-critical endpoints.
- Health/readiness probes and startup dependency checks in container runtime.

### Observability

- Correlation and trace propagation from gateway to downstream services.
- Error capture to Sentry from frontend and backend.
- Structured logs with request ID, user ID (if available), route, latency, status.

## 5. API and Contract Principles

- DTO-only public contracts; entities are never leaked.
- Additive-first evolution policy:
  - New fields are backward compatible.
  - Breaking changes require versioned endpoint and deprecation window.
- Error contract is machine-readable and consistent across services:
  - `code`, `message`, `details`, `trace_id`, `timestamp`.

## 6. Data Architecture Principles

- Primary keys are UUID across all aggregates.
- Mandatory audit columns:
  - `created_at`, `created_by`, `updated_at`, `updated_by`.
- Soft delete standard:
  - `deleted` boolean, `deleted_at` timestamp nullable.
- Index design follows top query patterns (search, ownership, status, and time windows).

## 7. Multi-Tenancy Readiness

The v1 blueprint is tenant-ready with minimal change:

- Add `tenant_id` to top-level business tables.
- Include tenant context in JWT claims.
- Apply tenant scoping in query repositories by default filter.

## 8. Security Ownership Matrix

| Concern | Gateway | Auth Service | CRM Service | Frontend |
|---|---|---|---|---|
| Token presence and basic format check | Yes | No | No | Client attach only |
| Access token issuing and refresh | No | Yes | No | Trigger flow |
| RBAC policy source of truth | No | Yes | Read/check | Cache-aware UI |
| Route-level throttling | Yes | No | No | No |
| Business permission enforcement | No | Optional | Yes | UI-level hints |

## 9. Deployment and Environment Strategy

- Local development through Docker Compose.
- CI/CD pipeline packages identical Docker images used in runtime.
- Environment configuration via runtime env vars only (no secrets in repo).
- Branch promotion and protected environment gates for deploy stages.

## 10. Implementation Constraints

- Java 21 + Spring Boot 3 + Maven.
- Next.js App Router + TypeScript.
- MySQL primary store and Redis cache/session.
- Spring Cloud Gateway for edge concerns.
- Sentry for frontend/backend issue visibility.
