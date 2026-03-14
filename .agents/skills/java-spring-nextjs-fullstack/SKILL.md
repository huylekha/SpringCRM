---
name: java-spring-nextjs-fullstack
description: Coordinates fullstack delivery across enterprise Spring Boot backend and Next.js frontend skills. Use for cross-layer features, API contracts, and end-to-end implementation planning.
---

# Java Spring + Next.js Fullstack

## Apply When
- User requests cross-layer features touching backend and frontend.
- Task requires API contract alignment, auth flow, or end-to-end integration.
- Work needs coordinated architecture decisions across services and UI.

## Delegation Model
- Use `backend-spring-enterprise` skill for backend implementation details.
- Use `frontend-nextjs-enterprise` skill for frontend architecture and UI patterns.
- Use `devops-gitlab-docker-cicd` skill for CI/CD, Docker, and deployment pipelines.
- Use `fullstack-testing-enterprise` skill for testing architecture, coverage policy, and CI test gate strategy.
- Apply shared governance rules for universal quality constraints.

## Fullstack Coordination Rules
- Start from API contract first (request/response/error + pagination).
- Keep backend DTOs and frontend types aligned.
- Define sync (OpenFeign) vs async (Kafka) boundaries before coding.
- Keep observability and error handling consistent across BE and FE.

## Delivery Expectations
- Generate production-grade code and explain key trade-offs briefly.
- Prefer maintainable structure over short-term shortcuts.
- Call out risks, missing tests, and rollout concerns explicitly.
