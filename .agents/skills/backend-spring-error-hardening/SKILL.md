---
name: backend-spring-error-hardening
description: Hardens Spring Boot backend services to enterprise and fintech standards by preventing HTTP 500, enforcing clean layering, secure API behavior, transaction safety, and JPA performance discipline.
---

# Backend Spring Error Hardening

## Apply When
- Implementing or refactoring backend APIs.
- Fixing frequent HTTP 500 issues.
- Improving exception handling, validation, and response consistency.

## Core Hardening Rules
- Keep controllers thin and service-driven.
- Use DTOs for all API boundaries.
- Use `@Valid` and validation annotations on request DTOs.
- Standardize output with `ApiResponse<T>`.
- Enforce global exception handling with `@RestControllerAdvice`.
- Never expose internal errors or stack traces to clients.
- Enforce stable machine-readable `error_code` for all API errors.

## Error Code Governance
- Use `<CATEGORY>_<NUMBER>` format with 4-digit zero padding.
- Keep centralized error code enum/registry.
- Map exceptions to codes in global exception handler.
- Never change or reuse released error codes.
- Ensure logs include `error_code`, `request_id`, and `user_id` when available.

## Data and Transaction Safety
- Keep `@Transactional` in service layer.
- Use `readOnly = true` for read flows.
- Map entities to DTOs before response.
- Use `Optional` safely with `orElseThrow`.

## Query and Performance Safety
- Prevent N+1 with fetch strategy review.
- Use pagination for list APIs.
- Use projections/caching where read load is high.

## Security and Stability Safety
- Use JWT + role-based access control for protected endpoints.
- Hash passwords with BCrypt and never log sensitive values.
- Apply rate limiting for sensitive APIs (login, OTP, payments).
- Use optimistic locking for conflict-prone update flows when needed.
