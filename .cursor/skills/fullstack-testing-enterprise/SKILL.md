---
name: fullstack-testing-enterprise
description: Designs production-grade testing architecture for Spring Boot and React/Next.js systems. Use when creating test strategy, coverage plans, reliability audits, and CI/CD quality gates for enterprise teams.
---

# Fullstack Testing Enterprise

## Apply When
- User asks for testing strategy, testing architecture, or go-live quality audit.
- User asks how to test backend/frontend at scale.
- User asks for CI testing gates, flaky-test reduction, or coverage policy.

## Strategy Baseline
- Use testing pyramid:
  - unit tests as largest layer
  - integration tests as confidence middle layer
  - e2e tests as narrow critical-path layer
- Optimize for speed, determinism, and reliability.

## Backend Stack
- Unit: JUnit + Mockito
- Integration/database: Testcontainers
- API contract/behavior: RestAssured (and/or MockMvc where suitable)

## Frontend Stack
- Unit/component: Jest + React Testing Library
- API mocking: MSW
- E2E: Playwright (or Cypress if team standard)

## Enterprise Rules
- Require explicit loading/error/empty state tests for data-driven UIs.
- Test data should use factories/fixtures/seed scripts.
- Keep CI gates strict for unit + integration, selective for e2e smoke/critical journeys.
- Track flaky tests and quarantine/fix quickly.

## Coverage Guidance
- Backend service logic > 80% directional target.
- Frontend component logic > 70% directional target.
- Prefer meaningful behavior coverage over vanity percentages.
