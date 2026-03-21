package com.company.platform.shared.cqrs;

import com.company.platform.shared.exception.BusinessException;
import com.company.platform.shared.exception.ErrorCode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Default implementation of CommandBus that handles command dispatching through a pipeline.
 * Supports pipeline behaviors for cross-cutting concerns like validation, logging, and
 * transactions.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultCommandBus implements CommandBus {

  private final List<CommandHandler<?, ?>> commandHandlers;
  private final List<PipelineBehavior<?, ?>> pipelineBehaviors;

  // Cache handlers by command type for performance
  private volatile Map<Class<?>, CommandHandler<?, ?>> handlerCache;

  @Override
  @SuppressWarnings("unchecked")
  public <TResponse> TResponse send(Command<TResponse> command) {
    log.debug("Processing command: {}", command.getClass().getSimpleName());

    CommandHandler<Command<TResponse>, TResponse> handler = findHandler(command);

    // Build pipeline with applicable behaviors
    List<PipelineBehavior<Command<TResponse>, TResponse>> applicableBehaviors =
        pipelineBehaviors.stream()
            .filter(behavior -> behavior.canHandle(command.getClass()))
            .map(behavior -> (PipelineBehavior<Command<TResponse>, TResponse>) behavior)
            .sorted(Comparator.comparingInt(PipelineBehavior::getOrder))
            .collect(Collectors.toList());

    // Execute pipeline
    return executePipeline(command, handler, applicableBehaviors, 0);
  }

  @SuppressWarnings("unchecked")
  private <TResponse> CommandHandler<Command<TResponse>, TResponse> findHandler(
      Command<TResponse> command) {
    if (handlerCache == null) {
      synchronized (this) {
        if (handlerCache == null) {
          handlerCache =
              commandHandlers.stream()
                  .collect(Collectors.toMap(CommandHandler::getCommandType, Function.identity()));
        }
      }
    }

    CommandHandler<?, ?> handler = handlerCache.get(command.getClass());
    if (handler == null) {
      throw new BusinessException(
          ErrorCode.SYSTEM_CONFIGURATION_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    return (CommandHandler<Command<TResponse>, TResponse>) handler;
  }

  private <TResponse> TResponse executePipeline(
      Command<TResponse> command,
      CommandHandler<Command<TResponse>, TResponse> handler,
      List<PipelineBehavior<Command<TResponse>, TResponse>> behaviors,
      int currentIndex) {

    if (currentIndex >= behaviors.size()) {
      // End of pipeline, execute the actual handler
      return handler.handle(command);
    }

    PipelineBehavior<Command<TResponse>, TResponse> currentBehavior = behaviors.get(currentIndex);

    return currentBehavior.handle(
        command, () -> executePipeline(command, handler, behaviors, currentIndex + 1));
  }
}
