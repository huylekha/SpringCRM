# API Gateway Error Contract v1

## 1. Purpose

This document defines the unified error response contract for the API Gateway and downstream services. All services must conform to this envelope to ensure frontend and integration clients can parse errors consistently.

## 2. Error Envelope Schema

Every error response across the platform uses this shape:

```json
{
  "code": "DOMAIN_ERROR_CODE",
  "message": "Human-readable description of the error.",
  "details": [
    {
      "field": "fieldName",
      "message": "Field-level error message."
    }
  ],
  "traceId": "correlation-or-trace-id",
  "timestamp": "2026-03-11T10:00:00Z"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| code | string | yes | Machine-readable error code, uppercased, prefixed by domain |
| message | string | yes | Human-readable summary |
| details | array | no | Field-level validation errors; present for 400/422 responses |
| traceId | string | yes | Distributed trace / X-Correlation-Id |
| timestamp | string (ISO 8601 UTC) | yes | Server timestamp when error occurred |

## 3. Error Code Naming Convention

Format: `{DOMAIN}_{ERROR_NAME}`

| Domain Prefix | Service/Layer |
|---|---|
| `AUTH_` | auth-service |
| `CRM_` | crm-service |
| `SEARCH_` | search infrastructure (inside crm-service or shared) |
| `GATEWAY_` | api-gateway |
| `SYSTEM_` | unhandled / internal server errors |

## 4. Gateway Error Codes

| Code | HTTP Status | Description | When |
|---|---|---|---|
| GATEWAY_UNAUTHORIZED | 401 | Token missing, malformed, or expired at gateway | Pre-validation filter rejects request |
| GATEWAY_FORBIDDEN | 403 | Token valid but route-level policy denied | Reserved for future route-level ACL |
| GATEWAY_RATE_LIMITED | 429 | Rate limit exceeded | Redis token-bucket threshold breached |
| GATEWAY_ROUTE_NOT_FOUND | 404 | No matching route for path | Request path doesn't match any route |
| GATEWAY_DOWNSTREAM_TIMEOUT | 504 | Downstream service did not respond in time | Timeout threshold exceeded |
| GATEWAY_DOWNSTREAM_UNAVAILABLE | 503 | Downstream service is unavailable | Circuit breaker open or connection refused |
| GATEWAY_BAD_GATEWAY | 502 | Invalid response from downstream | Downstream returned unparseable response |

### Rate Limit Response Headers

When `GATEWAY_RATE_LIMITED` is returned, include these headers:

| Header | Description |
|---|---|
| X-RateLimit-Limit | Maximum requests allowed in window |
| X-RateLimit-Remaining | Requests remaining in current window |
| X-RateLimit-Reset | Unix timestamp when window resets |
| Retry-After | Seconds until rate limit resets |

### Rate Limit Response Example

```json
{
  "code": "GATEWAY_RATE_LIMITED",
  "message": "Too many requests. Please retry later.",
  "details": [],
  "traceId": "gw-trace-abc-123",
  "timestamp": "2026-03-11T10:00:00Z"
}
```

## 5. Auth Service Error Codes

| Code | HTTP Status | Description |
|---|---|---|
| AUTH_INVALID_CREDENTIALS | 401 | Username or password is invalid |
| AUTH_ACCOUNT_LOCKED | 401 | Account locked due to failed login attempts |
| AUTH_ACCOUNT_INACTIVE | 401 | Account is deactivated |
| AUTH_TOKEN_EXPIRED | 401 | Access or refresh token has expired |
| AUTH_TOKEN_REVOKED | 401 | Refresh token has been revoked |
| AUTH_INSUFFICIENT_PERMISSION | 403 | User lacks required permission for operation |
| AUTH_PRIVILEGE_ESCALATION_DENIED | 403 | Non-SUPER_ADMIN attempting SUPER_ADMIN role assignment |
| AUTH_DUPLICATE_USERNAME | 409 | Username already exists |
| AUTH_DUPLICATE_EMAIL | 409 | Email already exists |
| AUTH_ROLE_NOT_FOUND | 404 | Referenced role does not exist |
| AUTH_SEED_ROLE_PROTECTED | 422 | Cannot delete seed/system roles |
| AUTH_LAST_ADMIN_PROTECTED | 422 | Cannot deactivate the last SUPER_ADMIN |
| AUTH_VALIDATION_FAILED | 400 | Request body validation failed (see details) |
| AUTH_PASSWORD_POLICY_VIOLATION | 422 | Password does not meet complexity requirements |

## 6. CRM Service Error Codes

| Code | HTTP Status | Description |
|---|---|---|
| CRM_RESOURCE_NOT_FOUND | 404 | Requested CRM entity does not exist or is soft-deleted |
| CRM_INVALID_STATE_TRANSITION | 422 | State transition violates lifecycle rules |
| CRM_DUPLICATE_REFERENCE | 409 | Unique code/reference already exists |
| CRM_FORBIDDEN_OPERATION | 403 | User lacks permission or ownership for this operation |
| CRM_VALIDATION_FAILED | 400 | Request body validation failed (see details) |
| CRM_REFERENTIAL_INTEGRITY_VIOLATION | 422 | Referenced entity does not exist or is not active |
| CRM_IMMUTABLE_RECORD | 422 | Record is in a terminal state and cannot be modified |

## 7. Search Error Codes

| Code | HTTP Status | Description |
|---|---|---|
| SEARCH_INVALID_FILTER_FIELD | 400 | Filter field is not in the whitelist for this module |
| SEARCH_INVALID_OPERATOR | 400 | Operator is not supported for the specified field type |
| SEARCH_INVALID_SORT_FIELD | 400 | Sort field is not in the whitelist for this module |
| SEARCH_PAGE_SIZE_EXCEEDED | 400 | Requested page size exceeds maximum (100) |

## 8. System Error Codes

| Code | HTTP Status | Description |
|---|---|---|
| SYSTEM_INTERNAL_ERROR | 500 | Unhandled server error (should be rare in production) |
| SYSTEM_SERVICE_UNAVAILABLE | 503 | Service temporarily unavailable |

## 9. HTTP Status Code Semantics

| Status | Usage |
|---|---|
| 200 | Successful read, update, state transition, login, refresh, logout |
| 201 | Successful resource creation |
| 204 | Successful deletion (no content) |
| 400 | Request validation failure (malformed input, invalid filter/sort) |
| 401 | Authentication failure (missing/expired token, bad credentials) |
| 403 | Authorization failure (insufficient permission, privilege escalation) |
| 404 | Resource not found (includes soft-deleted resources) |
| 409 | Conflict (duplicate unique constraint: code, username, email) |
| 422 | Business rule violation (invalid state transition, referential integrity, immutable record) |
| 429 | Rate limit exceeded |
| 500 | Unhandled internal error |
| 502 | Bad gateway (downstream returned invalid response) |
| 503 | Service unavailable (downstream or self) |
| 504 | Gateway timeout |

## 10. Validation Error Details Format

For 400 and 422 responses with field-level errors, the `details` array provides specific field violations:

```json
{
  "code": "CRM_VALIDATION_FAILED",
  "message": "Request validation failed.",
  "details": [
    { "field": "fullName", "message": "must not be blank" },
    { "field": "email", "message": "must be a valid email address" },
    { "field": "expectedValue", "message": "must be greater than or equal to 0" }
  ],
  "traceId": "crm-trace-xyz-789",
  "timestamp": "2026-03-11T10:00:00Z"
}
```

## 11. Correlation and Tracing

- Gateway generates `X-Correlation-Id` header if not present in incoming request.
- All services propagate `X-Correlation-Id` in logs and error responses as `traceId`.
- Frontend clients should log `traceId` from error responses for support debugging.

## 12. Frontend Error Handling Guidance

| Error Code Pattern | Frontend Behavior |
|---|---|
| `*_VALIDATION_FAILED` | Display field-level errors from `details` array |
| `AUTH_INVALID_CREDENTIALS` | Show login error message |
| `AUTH_TOKEN_EXPIRED` | Trigger silent refresh flow |
| `AUTH_TOKEN_REVOKED` | Force re-login |
| `AUTH_ACCOUNT_LOCKED` | Show locked account message |
| `CRM_INVALID_STATE_TRANSITION` | Show descriptive message from `message` field |
| `CRM_RESOURCE_NOT_FOUND` | Redirect to list or show "not found" view |
| `GATEWAY_RATE_LIMITED` | Show rate limit message with retry countdown |
| `GATEWAY_DOWNSTREAM_*` | Show service unavailable message |
| `SYSTEM_INTERNAL_ERROR` | Show generic error with traceId for support |
