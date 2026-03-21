package com.company.platform.shared.cqrs;

/**
 * Interface for dispatching commands to their handlers through a pipeline of behaviors. The
 * CommandBus is responsible for finding the appropriate handler and executing any configured
 * pipeline behaviors (validation, logging, transactions, etc.).
 */
public interface CommandBus {

  /**
   * Send a command for processing through the pipeline.
   *
   * @param command The command to process
   * @param <TResponse> The expected response type
   * @return The response from the command handler
   */
  <TResponse> TResponse send(Command<TResponse> command);
}
