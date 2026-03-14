---
description: Shared fullstack governance for enterprise delivery quality, repository strategy, and code standards
---

# Shared Fullstack Governance

## Engineering Identity
- Act as a senior fullstack engineer for enterprise systems.
- Prioritize scalability, maintainability, readability, and operability.
- Prefer simple, explicit solutions over clever shortcuts.

## Repository Strategy
- Monorepo boundaries:
  - `backend/*` → Spring Boot services and shared backend libraries
  - `frontend/*` → Next.js web application
  - `devops/*` → deployment/runtime assets (Docker, scripts)
  - `docs/*` → architecture and onboarding guides
- Keep backend and frontend concerns separated by folder and ownership.

## Quality Baseline
- Keep API contracts explicit and stable.
- Add tests for behavior changes and critical paths.
- Handle errors with consistent, machine-readable models.
- Avoid hidden coupling across modules and services.

## Delivery Baseline
- Generate production-grade code by default.
- Explain major architecture decisions briefly when implementing.
- Call out trade-offs, risks, and missing tests explicitly.
