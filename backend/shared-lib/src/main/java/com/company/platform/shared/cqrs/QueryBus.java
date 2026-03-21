package com.company.platform.shared.cqrs;

/**
 * Interface for dispatching queries to their handlers through a pipeline of behaviors. The QueryBus
 * is responsible for finding the appropriate handler and executing any configured pipeline
 * behaviors (validation, logging, caching, etc.). Query processing should be read-only and not
 * modify system state.
 */
public interface QueryBus {

  /**
   * Send a query for processing through the pipeline.
   *
   * @param query The query to process
   * @param <TResponse> The expected response type
   * @return The response from the query handler
   */
  <TResponse> TResponse send(Query<TResponse> query);
}
