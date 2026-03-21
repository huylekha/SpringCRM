package com.company.platform.shared.util;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that generates or extracts trace ID for distributed tracing. Trace ID is propagated via: -
 * MDC for automatic logging inclusion - Response header for client consumption - Request attribute
 * for controller access
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

  public static final String TRACE_ID_HEADER = "X-Trace-Id";
  public static final String LEGACY_HEADER = "X-Correlation-Id";
  public static final String TRACE_ID_MDC_KEY = "traceId";
  public static final String REQUEST_PATH_MDC_KEY = "requestPath";
  public static final String TRACE_ID_ATTRIBUTE = "traceId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = extractOrGenerateTraceId(request);

    try {
      // Add to MDC for automatic logging inclusion
      MDC.put(TRACE_ID_MDC_KEY, traceId);
      MDC.put(REQUEST_PATH_MDC_KEY, request.getRequestURI());

      // Add to response headers for propagation
      response.setHeader(TRACE_ID_HEADER, traceId);
      response.setHeader(LEGACY_HEADER, traceId); // Backward compatibility

      // Store in request attribute for controller access
      request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);

      filterChain.doFilter(request, response);
    } finally {
      // Clean up MDC to prevent memory leaks
      MDC.remove(TRACE_ID_MDC_KEY);
      MDC.remove(REQUEST_PATH_MDC_KEY);
    }
  }

  /**
   * Extract trace ID from request header or generate new one. Supports both new (X-Trace-Id) and
   * legacy (X-Correlation-Id) headers.
   */
  private String extractOrGenerateTraceId(HttpServletRequest request) {
    // Try new header first
    String traceId = request.getHeader(TRACE_ID_HEADER);

    // Fall back to legacy header for backward compatibility
    if (traceId == null || traceId.isBlank()) {
      traceId = request.getHeader(LEGACY_HEADER);
    }

    // Generate new trace ID if not provided
    if (traceId == null || traceId.isBlank()) {
      traceId = generateTraceId();
    }

    return traceId;
  }

  /** Generate 16-character hex trace ID. Format: lowercase hexadecimal, no hyphens */
  private String generateTraceId() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
  }
}
