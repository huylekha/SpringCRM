package com.company.platform.shared.cqrs;

/**
 * Interface for pipeline behaviors that can intercept command/query processing. Behaviors are
 * executed in a chain before the actual handler is invoked. Common behaviors include validation,
 * logging, transaction management, and caching.
 *
 * @param <TRequest> The request type (Command or Query)
 * @param <TResponse> The response type
 */
public interface PipelineBehavior<TRequest, TResponse> {

  /**
   * Handle the request with the pipeline behavior.
   *
   * @param request The request to process
   * @param next The next behavior or handler in the pipeline
   * @return The response from the pipeline
   */
  TResponse handle(TRequest request, RequestHandlerDelegate<TResponse> next);

  /**
   * Get the order/priority of this behavior in the pipeline. Lower numbers execute first.
   *
   * @return The execution order (0 = highest priority)
   */
  default int getOrder() {
    return 0;
  }

  /**
   * Check if this behavior should be applied to the given request type.
   *
   * @param requestType The request class type
   * @return true if this behavior should be applied
   */
  boolean canHandle(Class<?> requestType);
}
