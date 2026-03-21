package com.company.platform.shared.cqrs;

/**
 * Interface for command handlers in the CQRS architecture. Each command handler processes a
 * specific command type and returns a response.
 *
 * @param <TCommand> The command type this handler processes
 * @param <TResponse> The response type returned by this handler
 */
public interface CommandHandler<TCommand extends Command<TResponse>, TResponse> {

  /**
   * Handle the command and return a response.
   *
   * @param command The command to process
   * @return The response from processing the command
   */
  TResponse handle(TCommand command);

  /**
   * Get the command type this handler processes. Used for handler registration and dispatch.
   *
   * @return The command class type
   */
  Class<TCommand> getCommandType();
}
