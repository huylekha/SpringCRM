# Observability Refactoring Analysis & Plan

## Current State Analysis

### ✅ Already Implemented:
1. **Basic Trace ID System**
   - `CorrelationIdFilter` exists but has gaps
   - Using `X-Correlation-Id` header (should standardize to `X-Trace-Id`)
   - No MDC propagation for logging
   - No response header for trace propagation

2. **Error Handling**
   - `GlobalExceptionHandler` with i18n support
   - `ApiError` includes traceId field
   - Structured exception handling for Business, Auth, Validation errors

3. **Multi-Language Support**
   - `MessageService` with fallback
   - 4 language support (en, vi, ja, zh)
   - `ErrorCode` enum with 150+ codes

### ❌ Missing Components:

#### 1. MDC-Based Trace Propagation
- CorrelationIdFilter doesn't add trace ID to MDC
- Logs don't include trace ID automatically
- No thread-local context for trace access

#### 2. Structured Logging
- No Logback configuration (missing logback-spring.xml)
- Plain text logging instead of JSON
- Missing Logstash encoder dependency
- No automatic trace ID in log entries

#### 3. OpenTelemetry Integration
- No OpenTelemetry dependencies
- No distributed tracing spans
- No service-to-service trace propagation
- Missing custom span instrumentation

#### 4. Prometheus Metrics
- No Micrometer registry dependency
- No /actuator/prometheus endpoint
- No custom business metrics
- No request latency tracking

#### 5. Enhanced Error Response
- ApiError missing `path` field
- No HTTP method or endpoint tracking
- No user context in errors

#### 6. Frontend Trace Propagation
- No Axios interceptor configured
- No trace ID generation on frontend
- No error boundary with trace display

---

## Refactoring Plan

### Phase 1: Enhanced Trace ID System (Priority: CRITICAL)

#### Task 1.1: Upgrade CorrelationIdFilter → TraceIdFilter
**File:** `backend/shared-lib/src/main/java/com/company/platform/shared/util/TraceIdFilter.java`

**Changes:**
- Rename class: `CorrelationIdFilter` → `TraceIdFilter`
- Change header: `X-Correlation-Id` → `X-Trace-Id`
- Add MDC integration: `MDC.put("traceId", traceId)`
- Add `@Order(Ordered.HIGHEST_PRECEDENCE)` for priority
- Shorten trace ID: 32 chars → 16 chars
- Clean up MDC in finally block
- Store in request attribute for controller access

**Impact:** 
- All services using CorrelationIdFilter need update
- GlobalExceptionHandler needs header name change
- Tests need update

#### Task 1.2: Create TraceIdContext Utility
**File:** `backend/shared-lib/src/main/java/com/company/platform/shared/util/TraceIdContext.java`

**Purpose:**
- Centralized trace ID access via `getCurrentTraceId()`
- Thread-safe MDC wrapper
- Easy access for services and controllers

---

### Phase 2: Structured Logging (Priority: CRITICAL)

#### Task 2.1: Add Logstash Encoder Dependency
**File:** `backend/shared-lib/pom.xml`

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

#### Task 2.2: Create Logback Configuration
**File:** `backend/shared-lib/src/main/resources/logback-spring.xml`

**Features:**
- JSON console appender with Logstash encoder
- Auto-include traceId, userId from MDC
- Service name from spring.application.name
- Proper field naming (timestamp, level, message, stack_trace)

#### Task 2.3: Enhance GlobalExceptionHandler Logging
**File:** `backend/shared-lib/src/main/java/com/company/platform/shared/exception/GlobalExceptionHandler.java`

**Changes:**
- Add request path to all log statements
- Add HTTP method to logs
- Add user ID from SecurityContext
- Add error code to structured logs
- Use consistent log format across all handlers

---

### Phase 3: Enhanced ApiError Response (Priority: HIGH)

#### Task 3.1: Add Missing Fields to ApiError
**File:** `backend/shared-lib/src/main/java/com/company/platform/shared/exception/ApiError.java`

**New Fields:**
- `String path` - Request URI
- `String method` - HTTP method (GET, POST, etc.)

**Changes:**
```java
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {
    private final String code;
    private final String message;
    private final String traceId;
    private final String path;        // NEW
    private final String method;      // NEW (optional)
    @Builder.Default
    private final Instant timestamp = Instant.now();
    private final List<FieldError> details;
}
```

#### Task 3.2: Update GlobalExceptionHandler
- Extract `request.getRequestURI()` → `path`
- Extract `request.getMethod()` → `method`
- Add to all ApiError builder calls

---

### Phase 4: OpenTelemetry Integration (Priority: MEDIUM)

#### Task 4.1: Add OpenTelemetry Dependencies
**File:** `backend/shared-lib/pom.xml`

```xml
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-spring-boot-starter</artifactId>
    <version>1.32.0-alpha</version>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

#### Task 4.2: Create OpenTelemetry Configuration
**Files:**
- `backend/shared-lib/src/main/java/com/company/platform/shared/config/OpenTelemetryConfig.java`
- `backend/shared-lib/src/main/java/com/company/platform/shared/tracing/Traced.java` (annotation)
- `backend/shared-lib/src/main/java/com/company/platform/shared/tracing/TracingAspect.java`

#### Task 4.3: Add @Traced to Service Methods
**Target Services:**
- `UserService.getUser()`, `createUser()`, `updateUser()`
- `AuthenticationService.login()`, `refresh()`
- All repository operations (optional, via aspect)

---

### Phase 5: Prometheus Metrics (Priority: MEDIUM)

#### Task 5.1: Add Micrometer Dependencies
**File:** `backend/shared-lib/pom.xml`

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

#### Task 5.2: Enable Actuator Endpoints
**File:** `backend/auth-service/src/main/resources/application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    tags:
      application: ${spring.application.name}
    export:
      prometheus:
        enabled: true
```

#### Task 5.3: Create Metrics Configuration
**Files:**
- `backend/shared-lib/src/main/java/com/company/platform/shared/config/MetricsConfig.java`
- `backend/shared-lib/src/main/java/com/company/platform/shared/metrics/MetricsInterceptor.java`

#### Task 5.4: Add Custom Metrics to Services
- Request duration tracking
- Error count by error code
- Business metrics (user.created, login.success, etc.)

---

### Phase 6: Frontend Trace Propagation (Priority: HIGH)

#### Task 6.1: Update Axios Interceptor
**File:** `frontend/src/services/api/interceptors.ts`

**Changes:**
- Generate trace ID if not exists
- Store in sessionStorage
- Add to all requests via `X-Trace-Id` header
- Capture from response headers

#### Task 6.2: Create Error Boundary with Trace Display
**File:** `frontend/src/components/ErrorBoundary.tsx`

**Features:**
- Display trace ID prominently
- Copy-to-clipboard functionality
- Send error report to backend
- Reload button

---

### Phase 7: Testing (Priority: HIGH)

#### Task 7.1: Unit Tests
- `TraceIdFilterTest` - trace generation, propagation, MDC
- `TraceIdContextTest` - thread safety, null handling
- `StructuredLoggingTest` - JSON format, MDC inclusion
- `GlobalExceptionHandlerTest` - trace ID in errors

#### Task 7.2: Integration Tests
- End-to-end trace propagation test
- Frontend → Backend → Database trace correlation
- Metrics endpoint exposure test

---

## Implementation Order

### Sprint 1 (Week 1): Critical Path
1. ✅ Task 1.1: Upgrade to TraceIdFilter with MDC
2. ✅ Task 1.2: Create TraceIdContext utility
3. ✅ Task 2.1: Add Logstash dependency
4. ✅ Task 2.2: Create logback-spring.xml
5. ✅ Task 2.3: Enhance GlobalExceptionHandler logging
6. ✅ Task 3.1: Add path/method to ApiError
7. ✅ Task 3.2: Update GlobalExceptionHandler

**Deliverable:** Structured JSON logging with automatic trace correlation

### Sprint 2 (Week 2): Observability Foundation
1. ✅ Task 5.1: Add Micrometer dependencies
2. ✅ Task 5.2: Enable actuator endpoints
3. ✅ Task 5.3: Create metrics configuration
4. ✅ Task 6.1: Update frontend Axios interceptor
5. ✅ Task 6.2: Create error boundary
6. ✅ Task 7.1: Write unit tests

**Deliverable:** Prometheus metrics + Frontend trace propagation

### Sprint 3 (Week 3): Distributed Tracing
1. ✅ Task 4.1: Add OpenTelemetry dependencies
2. ✅ Task 4.2: Create OTel configuration
3. ✅ Task 4.3: Add @Traced annotations
4. ✅ Task 5.4: Add custom business metrics
5. ✅ Task 7.2: Write integration tests

**Deliverable:** Full distributed tracing with spans

---

## Breaking Changes

### API Changes:
- ❌ Header name change: `X-Correlation-Id` → `X-Trace-Id`
  - **Impact:** Frontend, API Gateway, external clients
  - **Migration:** Support both headers during transition period

### Response Format:
- ✅ ApiError gets new fields: `path`, `method` (backward compatible)
  - **Impact:** None - JSON ignores unknown fields

### Dependencies:
- ✅ New: logstash-logback-encoder, micrometer, opentelemetry
  - **Impact:** Maven build time +10-15 seconds
  - **Runtime:** +5-10ms per request overhead

---

## Rollback Plan

### If Issues Occur:

1. **Logging Issues:**
   - Remove logback-spring.xml
   - Fall back to default Spring Boot logging
   - Keep trace ID in MDC

2. **Metrics Issues:**
   - Disable Prometheus exporter in application.yml
   - Remove MetricsInterceptor from context
   - Keep actuator for health checks

3. **Tracing Issues:**
   - Disable OpenTelemetry auto-instrumentation
   - Remove @Traced annotations
   - Keep basic trace ID system

4. **Header Name Change:**
   - Revert TraceIdFilter to use X-Correlation-Id
   - Update frontend interceptor
   - Communicate to API consumers

---

## Success Metrics

### Post-Implementation KPIs:

1. **Mean Time to Resolution (MTTR):**
   - Before: ~2-4 hours (searching logs manually)
   - Target: < 5 minutes (search by trace ID)

2. **Log Search Speed:**
   - Before: grep through multiple log files
   - Target: Single Elasticsearch/Loki query by trace_id

3. **Production Debugging:**
   - Before: Add logs → redeploy → reproduce issue
   - Target: Find root cause with existing traces

4. **Error Correlation:**
   - Before: 60% of errors had no context
   - Target: 100% of errors have trace_id, path, user

5. **Monitoring Coverage:**
   - Before: Basic health checks only
   - Target: Request latency, error rate, business metrics

---

## Documentation Updates Needed

1. **README.md:**
   - Add observability section
   - Document trace ID system
   - Link to Grafana dashboards

2. **API Documentation:**
   - Document X-Trace-Id header
   - Show sample error responses with trace IDs
   - Explain how to debug with trace IDs

3. **Runbooks:**
   - "How to debug production errors with trace IDs"
   - "How to read OpenTelemetry traces"
   - "Alert response procedures"

4. **Architecture Diagrams:**
   - Update with trace flow
   - Add monitoring stack diagram
   - Show metrics collection points

---

## Estimated Effort

| Phase | Tasks | Effort | Risk |
|-------|-------|--------|------|
| Phase 1: Trace ID | 2 | 1 day | Low |
| Phase 2: Logging | 3 | 2 days | Low |
| Phase 3: ApiError | 2 | 0.5 day | Low |
| Phase 4: OpenTelemetry | 3 | 3 days | Medium |
| Phase 5: Prometheus | 4 | 2 days | Low |
| Phase 6: Frontend | 2 | 1 day | Low |
| Phase 7: Testing | 2 | 2 days | Low |
| **Total** | **18** | **11.5 days** | **Low-Medium** |

**Note:** Can be done in 3 sprints (1 week each) with parallel work.

---

## Next Steps

1. **Review this plan** with team
2. **Prioritize phases** based on business needs
3. **Start with Phase 1** (most critical, least risk)
4. **Set up staging environment** for testing
5. **Create feature branch**: `feature/observability-refactor`
6. **Begin implementation** following task order

Would you like me to proceed with Phase 1 implementation now?
