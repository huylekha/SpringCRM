package com.company.platform.shared.cqrs;

/**
 * Functional interface representing the next step in the pipeline. Used by pipeline behaviors to
 * continue processing to the next behavior or final handler.
 *
 * @param <TResponse> The response type
 */
@FunctionalInterface
public interface RequestHandlerDelegate<TResponse> {

  /**
   * Continue processing to the next step in the pipeline.
   *
   * @return The response from the next step
   */
  TResponse handle();
}
