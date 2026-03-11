# Cursor Ruleset Organization

This project uses a consolidated ruleset to avoid fragmentation.

## Core Rules (routing always loaded, domain rules file-targeted)

### Shared
- `00-shared-fullstack-governance.mdc`
- `01-skill-auto-routing.mdc`
- `02-delivery-strict-mode.mdc`
- `03-delivery-strict-lite.mdc` (on-demand)
- `04-delivery-strict-full.mdc` (on-demand)

### Backend (Active)
- `10-backend-spring-enterprise-core.mdc`
- `11-backend-sync-async-integration.mdc`
- `12-backend-data-mysql-oracle.mdc`

### Backend (Ultimate Canonical / On-demand)
- `spring-architecture.mdc` (Ultimate Production Rules - Architecture, 15 rules)
- `spring-api-rules.mdc` (Ultimate Production Rules - API, 10 rules)
- `spring-error-handling.mdc` (Ultimate Production Rules - Error Handling, 10 rules)
- `spring-null-safety.mdc` (Ultimate Production Rules - Null Safety, 10 rules)
- `spring-transaction-rules.mdc` (Ultimate Production Rules - Transaction, 10 rules)
- `spring-db-performance.mdc` (Ultimate Production Rules - DB Performance, 10 rules)
- `spring-security.mdc` (Ultimate Production Rules - Security, 10 rules)
- `spring-observability.mdc` (Ultimate Production Rules - Observability, 10 rules)
- `spring-resilience.mdc` (Ultimate Production Rules - Resilience, 10 rules)
- `spring-testing-ci.mdc` (Ultimate Production Rules - Testing+CI, 10 rules)
- Note: these ultimate files are on-demand (`alwaysApply: false`, no `globs`) to prevent duplicate context loading.

### Backend (Cleanup Status)
- Legacy redirect files have been removed in hard-cleanup mode.
- Use only the Ultimate canonical files above for architecture, API, error, transaction, performance, security, observability, resilience, and testing guidance.

### Frontend
- `20-frontend-nextjs-enterprise-core.mdc`
- `21-frontend-reusable-patterns.mdc`

### Frontend (Specialized / On-demand)
- `frontend-react-anti-patterns-scale-fail.mdc` (React Enterprise Checklist - 100 rules)

### DevOps
- `30-devops-gitlab-docker-cicd-core.mdc`

### Testing (Specialized / On-demand)
- `fullstack-testing-strategy-enterprise.mdc`

## Template Rules (on-demand generation)
- `openfeign-resilience-template.mdc`
- `kafka-outbox-template.mdc`
- `spring-profile-config-templates.mdc`
- `gitlab-docker-cicd-template.mdc`
- `spring-error-code-template.mdc`
- `frontend-user-management-feature-template.mdc`
- `frontend-access-control-template.mdc`

Use template rules when asking for skeleton/boilerplate code generation.

Strict mode usage:
- default behavior: strict-lite via `02-delivery-strict-mode.mdc`
- explicit phrase `strict-full`: apply enterprise full gates from `04-delivery-strict-full.mdc`
- quick aliases:
  - `#SL` => force strict-lite
  - `#SF` => force strict-full
  - if both exist, `#SF` wins

Role tag routing quick reference:
- `#BE` => backend specialist
- `#FE` => frontend specialist
- `#MB` => mobile specialist (React Native)
- `#QC` or `#Test` => QA/testing specialist (same alias mapping)
- `#OPS` => DevOps CI/CD specialist (15+ years)
- `#BA` => business analyst specialist
- `#SA` => solution architect specialist
- `#PM` => product manager specialist
- if multiple role tags exist, the last role tag is used as primary context

Activation policy:
- Keep only shared routing/governance rules as `alwaysApply: true`:
  - `00-shared-fullstack-governance.mdc`
  - `01-skill-auto-routing.mdc`
  - `02-delivery-strict-mode.mdc`
- Keep domain implementation rules (BE/FE/DevOps/Testing) as `alwaysApply: false` and rely on:
  - role tags (`#BE/#FE/#MB/#QC/#Test/#OPS/#BA/#SA/#PM`) for specialist context
  - `globs` for domain-specific rule loading
- This keeps behavior consistent across roles and avoids context bloat.

Guideline:
- shared concerns in `00-*`
- backend concerns in `10-*`
- frontend concerns in `20-*`
- devops concerns in `30-*`
