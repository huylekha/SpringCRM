# Enterprise CRM Platform - Summary

## 1. Objective

This document is the master index and execution baseline for building a production-grade CRM platform with:

- Backend: Java 21, Spring Boot 3, Maven, Hibernate, QueryDSL, PostgreSQL, Redis, Spring Security JWT, Spring Cloud Gateway.
- Frontend: Next.js App Router, TypeScript, Zustand, TanStack Query, Axios, React Hook Form, Zod, shadcn/ui, Tailwind CSS, TanStack Table, Recharts.
- DevOps: Docker, Docker Compose, GitLab CI first (GitHub Actions compatible design), Sentry observability.

## 2. Scope and Non-Goals

### In Scope (v1 Blueprint)

- Enterprise architecture and service boundaries.
- API contracts and search contract pattern.
- RBAC data model and authorization policy.
- Folder structure and engineering conventions for backend and frontend.
- Local container topology and CI/CD reference design.
- Delivery roadmap with dependencies and risk controls.

### Non-Goals (this phase)

- No source-code scaffolding.
- No migration execution on runtime database.
- No actual CI/CD deployment execution.

## 3. Mandatory Architecture Decisions

- `AD-001`: Architecture style is microservices + API Gateway.
- `AD-002`: Backend service internals follow Clean Architecture layering.
- `AD-003`: PostgreSQL is the primary transactional data store for all services.
- `AD-004`: Redis is used for token/session support, rate-limit counters, and cache.
- `AD-005`: All external APIs expose DTO only; entities are never exposed directly.
- `AD-006`: Search endpoints use QueryDSL with dynamic filters, sort, pagination.
- `AD-007`: UUID is the primary key standard for all domain entities.
- `AD-008`: Audit (`created_at`, `created_by`, `updated_at`, `updated_by`) and soft delete (`deleted`, `deleted_at`) are mandatory.
- `AD-009`: Observability is mandatory with Sentry and distributed tracing propagation.
- `AD-010`: CI/CD requires quality gates before Docker push and deployment.
- `AD-011`: List endpoints use `GET` with query parameters for simple filters; `POST /search` for complex QueryDSL filters.
- `AD-012`: Error responses follow a unified envelope (`code`, `message`, `details`, `traceId`, `timestamp`) across all services.
- `AD-013`: RBAC enforcement at service level with data-scope filtering (ALL/TEAM/OWN) per role.
- `AD-014`: Soft-delete via `DELETE` returns 204; soft-deleted records excluded from GET/list/search.

## 4. Service Landscape

- `auth-service` (port `8081`)
  - Authentication, JWT issue/refresh/revoke, user-role-claim-permission management.
- `crm-service` (port `8082`)
  - CRM domain modules: customer, lead, opportunity, activity, task, note.
- `api-gateway` (port `8080`)
  - Routing, token validation, rate limiting, logging, tracing.
- `frontend` (port `3000`)
  - App Router UI with role-aware access control and CRM workflows.
- Supporting services
  - `postgres` (port `5432`), `redis` (port `6379`).

## 5. Documentation Index (Canonical Set)

| Area | Document | Purpose |
|---|---|---|
| System Architecture | [architecture/system-architecture.md](architecture/system-architecture.md) | Target architecture, NFRs, boundary rules, deployment topology |
| Diagrams | [architecture/microservice-architecture-diagram.md](architecture/microservice-architecture-diagram.md) | Mermaid context/container/sequence views |
| Backend Structure | [backend/backend-folder-structure.md](backend/backend-folder-structure.md) | Service and package layout with clean architecture contracts |
| Frontend Structure | [frontend/frontend-folder-structure.md](frontend/frontend-folder-structure.md) | App Router + feature modular structure |
| Data | [data/database-schema.md](data/database-schema.md) | PostgreSQL schema, indexing strategy, audit and soft-delete standard |
| Auth | [services/auth-service-design.md](services/auth-service-design.md) | Auth module contracts, JWT lifecycle, security controls |
| CRM | [services/crm-service-design.md](services/crm-service-design.md) | Domain aggregates, APIs, lifecycle workflows |
| Gateway | [services/api-gateway-design.md](services/api-gateway-design.md) | Route policy, auth pass-through, resilience and observability |
| Search | [architecture/search-api-architecture.md](architecture/search-api-architecture.md) | QueryDSL contract, dynamic filter DSL, performance guardrails |
| RBAC | [architecture/rbac-model.md](architecture/rbac-model.md) | Authorization entities and evaluation flow |
| Docker Runtime | [devops/docker-deployment-architecture.md](devops/docker-deployment-architecture.md) | Image strategy and runtime deployment pattern |
| Local Compose | [devops/docker-compose-example.md](devops/docker-compose-example.md) | Full local stack compose reference |
| CI/CD | [devops/cicd-pipeline-design.md](devops/cicd-pipeline-design.md) | Build-test-package-push-deploy pipeline design |
| Roadmap | [roadmap/development-roadmap.md](roadmap/development-roadmap.md) | Delivery phases, dependencies, acceptance gates |
| Feature Catalog | [api/feature-catalog-v1.md](api/feature-catalog-v1.md) | Module capabilities, business rules, edge cases, acceptance criteria, endpoint traceability |
| Auth OpenAPI | [api/auth-openapi-v1.yaml](api/auth-openapi-v1.yaml) | OpenAPI 3.0 spec for auth-service endpoints and DTOs |
| CRM OpenAPI | [api/crm-openapi-v1.yaml](api/crm-openapi-v1.yaml) | OpenAPI 3.0 spec for crm-service endpoints and DTOs |
| Error Contract | [api/gateway-error-contract-v1.md](api/gateway-error-contract-v1.md) | Unified error envelope, error codes, HTTP status semantics |

## 6. Execution Sequence

1. Foundation
   - Repo structure (`backend/*`, `frontend/*`, `devops/*`, `docs/*`), platform conventions, baseline tooling.
2. Identity and Security
   - Implement `auth-service` and RBAC entities + JWT lifecycle.
3. CRM Core
   - Build core aggregates and transactional APIs.
4. Integration Edge
   - Enable `api-gateway` policies, limits, trace, and route governance.
5. Frontend Delivery
   - Implement auth flow, dashboard modules, and CRM workflows.
6. Search and Analytics
   - Dynamic QueryDSL filters, pageable tables, chart-ready endpoints.
7. DevOps and Release
   - Docker multi-stage images, compose local stack, CI/CD to environments.
8. Hardening
   - Performance tuning, security checks, chaos scenarios, release readiness.

## 7. Risk Heatmap and Mitigation

| Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|
| Over-scoped v1 contracts | High | Medium | Freeze v1 API scope and enforce ADR updates for contract changes |
| Security ownership ambiguity (gateway vs service) | High | Medium | Define explicit authN/authZ responsibility matrix in gateway/auth docs |
| Search API performance regressions | High | Medium | Whitelisted filter fields, index-first design, query plan review gates |
| CI/CD drift from runtime expectations | Medium | Medium | Keep Docker runtime and CI build paths identical |
| Multi-team dependency blocking | Medium | High | Enforce phase gates and shared milestones in roadmap |

## 8. Validation Gate (Strict-Full)

Before implementation kickoff, confirm:

- Every required architecture document exists and is internally consistent.
- API and data contracts are traceable from summary to service-level documents.
- Security, observability, and rollback are explicitly defined.
- Delivery roadmap includes owner assumptions, dependencies, and acceptance criteria.

## 9. Rollback Strategy for Architecture Decisions

- Decision rollback is handled by updating impacted design docs plus a decision log entry in this file.
- If architecture consensus fails, rollback by reverting `docs/` package as one atomic change set.
- No partial rollback for security-critical decisions (`JWT`, `RBAC`, `Gateway policy`) without a replacement decision.

## 10. Change Control

Use this format for major design revisions:

| Date | Decision ID | Change | Reason | Impacted Docs | Approved By |
|---|---|---|---|---|---|
| 2026-03-11 | AD-011..014 | Added GET list endpoints, unified error envelope, RBAC data-scope matrix, soft-delete convention | Standardize API contract for BE/FE/QA implementation readiness | crm-service-design.md, auth-service-design.md, rbac-model.md, feature-catalog-v1.md, auth-openapi-v1.yaml, crm-openapi-v1.yaml, gateway-error-contract-v1.md | SA/BA |
| YYYY-MM-DD | AD-XXX | Description | Business/Technical reason | List of files | Role/Team |

## 11. Cursor Rules Governance Snapshot

- Ruleset status: hard-cleanup completed, no legacy redirect rules remain.
- Canonical backend ultimate rules are centralized in `.cursor/rules/`:
  - `spring-architecture.mdc`
  - `spring-api-rules.mdc`
  - `spring-error-handling.mdc`
  - `spring-null-safety.mdc`
  - `spring-transaction-rules.mdc`
  - `spring-db-performance.mdc`
  - `spring-security.mdc`
  - `spring-observability.mdc`
  - `spring-resilience.mdc`
  - `spring-testing-ci.mdc`
- Routing model:
  - strict tags: `#SL` (strict-lite), `#SF` (strict-full), with `#SF` priority.
  - role tags: `#BE`, `#FE`, `#MB`, `#QC`/`#Test`, `#OPS`, `#BA`, `#SA`, `#PM`.
  - if multiple role tags are present, the last role tag is primary context.
- Activation policy:
  - only shared governance/routing rules are always-on.
  - domain rules are loaded by role context plus file scope to avoid context bloat.
