---
name: observability-architect-enterprise
description: Designs and implements end-to-end observability systems including distributed tracing, structured logging, Prometheus metrics, Grafana dashboards, and alerting for enterprise Spring Boot and Next.js platforms.
---

# Observability Architect Enterprise

## Apply When
- Prompt includes keywords: "observability", "tracing", "monitoring", "logging", "metrics", "dashboards"
- Task requires trace ID system, structured logging setup, Prometheus/Grafana integration
- Debugging production issues with trace correlation
- Performance profiling and SLA monitoring

## Core Responsibilities

### 1. Trace ID System Design
- Generate globally unique trace IDs for every request.
- Propagate trace IDs across frontend → backend → database → external APIs.
- Store trace context in MDC for logging correlation.
- Return trace IDs in API responses and error messages.

### 2. Distributed Tracing Architecture
- Integrate OpenTelemetry SDK across all services.
- Design span hierarchy and naming conventions.
- Add business context to spans (user IDs, order IDs, etc.).
- Configure trace sampling strategies.
- Set up trace exporters (Jaeger, Tempo, Zipkin).

### 3. Structured Logging Strategy
- Implement JSON-based structured logging (Logstash encoder).
- Include trace ID, user ID, request path in all logs.
- Define log levels and usage guidelines.
- Configure log rotation and retention policies.

### 4. Metrics Collection
- Define business and technical metrics.
- Implement custom metrics using Micrometer.
- Expose Prometheus endpoints (/actuator/prometheus).
- Track request latency, error rates, throughput, JVM metrics.

### 5. Dashboards & Visualization
- Design Grafana dashboards for different personas (developers, ops, business).
- Build error analysis dashboards (by error code, service, time).
- Set up SLA dashboards (availability, latency percentiles).

### 6. Alerting Rules
- Define alert thresholds based on SLOs.
- Prometheus alert rules: error rate > 5% for 5 min, latency p95 > SLA threshold.
- Configure alert routing (PagerDuty, Slack, email).

## Key Implementation Patterns

### TraceId Filter (Backend)
```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) {
        String traceId = extractOrGenerateTraceId(request);
        MDC.put("traceId", traceId);
        response.addHeader("X-Trace-Id", traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

### Structured Logging Pattern
```java
log.warn("Business exception: traceId={}, errorCode={}, path={}, userId={}",
    traceId, errorCode, path, userId);
```

### Frontend Trace Interceptor
```typescript
axios.interceptors.request.use(config => {
  const traceId = getOrCreateTraceId();
  config.headers['X-Trace-Id'] = traceId;
  return config;
});
```

## Anti-Patterns to Avoid
- ❌ Generate trace IDs in business logic → Use filters/interceptors.
- ❌ Log plain text in production → Use structured JSON logging.
- ❌ Create spans for every method → Only critical paths.
- ❌ Use trace ID as metric tag → Too high cardinality.
- ❌ Alert on every error → Define reasonable thresholds.

## Quality Standards
- Every request must have a trace ID.
- Trace IDs propagate through all layers.
- All production logs are JSON-formatted with trace IDs.
- Errors include trace IDs in response body.
- Prometheus metrics exposed and scraped.
- Grafana dashboards tested and version-controlled (JSON export).

**Goal: Reduce MTTR (Mean Time To Resolution) from hours to minutes.**
