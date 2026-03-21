package com.company.platform.shared.cqrs;

/**
 * Interface for query handlers in the CQRS architecture. Each query handler processes a specific
 * query type and returns a response. Query handlers should be read-only and not modify system
 * state.
 *
 * @param <TQuery> The query type this handler processes
 * @param <TResponse> The response type returned by this handler
 */
public interface QueryHandler<TQuery extends Query<TResponse>, TResponse> {

  /**
   * Handle the query and return a response. This method should be read-only and not modify system
   * state.
   *
   * @param query The query to process
   * @return The response from processing the query
   */
  TResponse handle(TQuery query);

  /**
   * Get the query type this handler processes. Used for handler registration and dispatch.
   *
   * @return The query class type
   */
  Class<TQuery> getQueryType();
}
