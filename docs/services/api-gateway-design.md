# API Gateway Design

## 1. Responsibility

`api-gateway` is the edge control plane responsible for:

- request routing
- token pre-validation
- rate limiting
- request/response logging
- trace/correlation propagation

It does not own business domain logic.

## 2. Route Topology

### Route Groups

- `/auth/**` -> `auth-service`
- `/crm/**` -> `crm-service`
- `/internal/health/**` -> service health paths (restricted)

### Versioning Convention

- External API prefix: `/api/v1`.
- Internal route translation handled at gateway filter/router level.

## 3. Security Policy

### AuthN at Gateway

- Validate token existence and basic signature/expiry constraints.
- Public allow-list only for:
  - `/auth/login`
  - `/auth/refresh`
  - optional docs/health endpoints by environment policy.

### AuthZ Delegation

- Gateway passes identity context headers downstream after validation.
- Service-level authorization remains mandatory for sensitive operations.

## 4. Rate Limiting

- Redis-backed token bucket limiter keyed by:
  - user ID (authenticated)
  - IP address (unauthenticated routes)
- Example policy:
  - login endpoint stricter than normal CRM read endpoints.
- Return `429` with retry metadata.

## 5. Request Logging and Tracing

- Generate/propagate `X-Correlation-Id`.
- Include in logs:
  - route ID
  - method/path
  - status code
  - latency
  - user ID when available
- Forward tracing headers to downstream services for end-to-end visibility.

## 6. Resilience Policies

- Timeouts per route category:
  - auth routes lower timeout
  - CRM search routes slightly higher timeout.
- Retry:
  - idempotent GET endpoints only, limited attempts.
- Circuit breaker:
  - open on repeated downstream failures.
- Fallback:
  - standardized upstream error response with trace ID.

## 7. Error Contract

Gateway errors follow the unified error envelope defined in [api/gateway-error-contract-v1.md](../api/gateway-error-contract-v1.md).

Gateway-level error payload:

```json
{
  "code": "GATEWAY_RATE_LIMITED",
  "message": "Too many requests. Please retry later.",
  "details": [],
  "traceId": "trace-id",
  "timestamp": "2026-03-11T10:00:00Z"
}
```

Common gateway codes:

- `GATEWAY_UNAUTHORIZED` (401)
- `GATEWAY_FORBIDDEN` (403)
- `GATEWAY_RATE_LIMITED` (429)
- `GATEWAY_ROUTE_NOT_FOUND` (404)
- `GATEWAY_DOWNSTREAM_TIMEOUT` (504)
- `GATEWAY_DOWNSTREAM_UNAVAILABLE` (503)
- `GATEWAY_BAD_GATEWAY` (502)

Rate limit responses include headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `Retry-After`.

## 8. Configuration Segments

- `route` definitions by service ID.
- `filter` chain:
  - correlation filter
  - auth pre-check filter
  - rate-limit filter
  - logging and metrics filter
- environment-specific policy settings in profile config files.

## 9. Operational Readiness

- Readiness checks:
  - route registry loaded
  - Redis connectivity for rate limit.
- Alerting:
  - high 5xx rate
  - high 429 spike
  - sustained downstream timeout.
- Sentry integration for unhandled filter exceptions.

## 10. Acceptance Criteria

- Auth and CRM routes correctly forward to target services.
- Public and protected routes are enforced by policy.
- Rate limiting and trace propagation are active in integration tests.
- Gateway failure responses are standardized and observable.
