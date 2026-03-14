---
description: Role-tag driven skill routing for SpringCRM fullstack development
---

# Antigravity Skill Auto-Routing Rules

## IMPORTANT: Read this workflow before any implementation task

Antigravity automatically routes to the appropriate skill based on role tags in user prompts.

## Tag-Driven Routing

Supported role tags → corresponding skill to load:
- `#BE` → `backend-spring-enterprise`
- `#FE` → `frontend-nextjs-enterprise`
- `#MB` → `mobile-react-native-enterprise`
- `#QC` or `#Test` → `qa-testing-enterprise`
- `#OPS` → `devops-gitlab-docker-cicd`
- `#BA` → `business-analyst-enterprise`
- `#SA` → `solution-architect-enterprise`
- `#PM` → `product-manager-enterprise`

## Strict Mode Tags
- `#SL` → Strict-Lite (default): minimal gates, smallest safe change
- `#SF` → Strict-Full: full governance, checkpoints, rollback plan
- If both exist, `#SF` wins.
- Strict mode resolves BEFORE role routing.

## Tag Conflict Rule
- If multiple role tags exist in one prompt, **the last role tag is the primary context**.
- Example: `#BE ... #SA ...` → use `solution-architect-enterprise`.

## Fallback Routing (No role tag provided)
- Testing strategy/architecture/audit: apply `fullstack-testing-enterprise`
- Backend architecture/design for large systems: apply `backend-spring-architecture-large-scale`
- Backend hardening/anti-500: apply `backend-spring-error-hardening`
- Backend-only requests: apply `backend-spring-enterprise`
- Frontend audit/review/refactor: apply `frontend-nextjs-enterprise`
- Frontend-only requests: apply `frontend-nextjs-enterprise`
- CI/CD, Docker, deploy, pipeline: apply `devops-gitlab-docker-cicd`
- Observability/tracing/monitoring: apply `observability-architect-enterprise`
- Cross-layer requests (BE + FE + integration): apply `java-spring-nextjs-fullstack`

## Routing Behavior
1. Check for `#SF` or `#SL` → apply strict mode first
2. Check for last role tag → route to skill
3. If no tag → apply fallback routing by intent
4. For mixed requirements: combine `java-spring-nextjs-fullstack` with specific skill

## Strict-Lite Baseline (Always)
- Before coding: define objective, impacted files, and test intent.
- Prefer smallest safe change. Avoid unrelated refactors.
- Report what was changed and what was verified.

## Strict-Full Gates (trigger with #SF or high-risk tasks)
- Apply when: architecture changes, security changes, CI/CD, migration, production rollout
- Apply when: changes touch many modules or high regression risk
- Include: checkpoint plan, rollback strategy, verification evidence

## Safety Rules (Always)
- Never hardcode secrets or credentials.
- Never use destructive commands unless explicitly requested.
- Never silently skip validation or deployment-critical checks.
