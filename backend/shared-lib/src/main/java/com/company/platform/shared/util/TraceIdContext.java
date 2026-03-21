package com.company.platform.shared.util;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.MDC;

/**
 * Thread-safe utility for accessing current trace ID from MDC. Provides centralized access to trace
 * context for services and controllers.
 */
public final class TraceIdContext {

  private static final String TRACE_ID_MDC_KEY = "traceId";
  private static final String UNKNOWN_TRACE_ID = "unknown";

  private TraceIdContext() {
    // Utility class
  }

  /**
   * Get current trace ID from MDC. Returns "unknown" if not set (should never happen with
   * TraceIdFilter).
   */
  @Nonnull
  public static String getCurrentTraceId() {
    String traceId = MDC.get(TRACE_ID_MDC_KEY);
    return traceId != null ? traceId : UNKNOWN_TRACE_ID;
  }

  /** Get current trace ID from MDC, or null if not set. */
  @Nullable
  public static String getCurrentTraceIdOrNull() {
    return MDC.get(TRACE_ID_MDC_KEY);
  }

  /**
   * Manually set trace ID in MDC. Only use for async operations or when TraceIdFilter doesn't run.
   */
  public static void setTraceId(@Nonnull String traceId) {
    if (traceId == null || traceId.isBlank()) {
      throw new IllegalArgumentException("Trace ID cannot be null or blank");
    }
    MDC.put(TRACE_ID_MDC_KEY, traceId);
  }

  /** Clear trace ID from MDC. Automatically called by TraceIdFilter cleanup. */
  public static void clear() {
    MDC.remove(TRACE_ID_MDC_KEY);
  }

  /** Check if trace ID is currently set. */
  public static boolean hasTraceId() {
    return MDC.get(TRACE_ID_MDC_KEY) != null;
  }
}
