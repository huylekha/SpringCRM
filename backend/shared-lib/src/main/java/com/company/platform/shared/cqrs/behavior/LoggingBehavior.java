package com.company.platform.shared.cqrs.behavior;

import com.company.platform.shared.cqrs.PipelineBehavior;
import com.company.platform.shared.cqrs.RequestHandlerDelegate;
import com.company.platform.shared.util.TraceIdContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Pipeline behavior that logs request processing with timing and context information. Logs slow
 * operations (>500ms) at WARN level for performance monitoring.
 */
@Component
@Slf4j
public class LoggingBehavior<TRequest, TResponse> implements PipelineBehavior<TRequest, TResponse> {

  private static final long SLOW_OPERATION_THRESHOLD_MS = 500L;

  @Override
  public TResponse handle(TRequest request, RequestHandlerDelegate<TResponse> next) {
    String requestType = request.getClass().getSimpleName();
    String traceId = TraceIdContext.getCurrentTraceId();
    String userId = getCurrentUserId();

    log.info("Processing request: type={}, traceId={}, userId={}", requestType, traceId, userId);

    long startTime = System.currentTimeMillis();

    try {
      TResponse response = next.handle();
      long duration = System.currentTimeMillis() - startTime;

      if (duration > SLOW_OPERATION_THRESHOLD_MS) {
        log.warn(
            "Slow operation detected: type={}, duration={}ms, traceId={}, userId={}",
            requestType,
            duration,
            traceId,
            userId);
      } else {
        log.info(
            "Request completed: type={}, duration={}ms, traceId={}, userId={}",
            requestType,
            duration,
            traceId,
            userId);
      }

      return response;

    } catch (Exception ex) {
      long duration = System.currentTimeMillis() - startTime;
      log.error(
          "Request failed: type={}, duration={}ms, traceId={}, userId={}, error={}",
          requestType,
          duration,
          traceId,
          userId,
          ex.getMessage(),
          ex);
      throw ex;
    }
  }

  @Override
  public int getOrder() {
    return 1000; // Execute last (highest number) to capture total duration
  }

  @Override
  public boolean canHandle(Class<?> requestType) {
    // Apply logging to all requests
    return true;
  }

  /**
   * Get current user ID from SecurityContext for logging. Returns "anonymous" if not authenticated.
   */
  private String getCurrentUserId() {
    try {
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null
          && authentication.isAuthenticated()
          && !"anonymousUser".equals(authentication.getPrincipal())) {
        return authentication.getName();
      }
    } catch (Exception e) {
      // Ignore - SecurityContext not available
    }
    return "anonymous";
  }
}
