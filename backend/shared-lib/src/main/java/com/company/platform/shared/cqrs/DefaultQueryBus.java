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
 * Default implementation of QueryBus that handles query dispatching through a pipeline. Supports
 * pipeline behaviors for cross-cutting concerns like validation, logging, and caching.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultQueryBus implements QueryBus {

  private final List<QueryHandler<?, ?>> queryHandlers;
  private final List<PipelineBehavior<?, ?>> pipelineBehaviors;

  // Cache handlers by query type for performance
  private volatile Map<Class<?>, QueryHandler<?, ?>> handlerCache;

  @Override
  @SuppressWarnings("unchecked")
  public <TResponse> TResponse send(Query<TResponse> query) {
    log.debug("Processing query: {}", query.getClass().getSimpleName());

    QueryHandler<Query<TResponse>, TResponse> handler = findHandler(query);

    // Build pipeline with applicable behaviors (excluding transaction behaviors for queries)
    List<PipelineBehavior<Query<TResponse>, TResponse>> applicableBehaviors =
        pipelineBehaviors.stream()
            .filter(behavior -> behavior.canHandle(query.getClass()))
            .map(behavior -> (PipelineBehavior<Query<TResponse>, TResponse>) behavior)
            .sorted(Comparator.comparingInt(PipelineBehavior::getOrder))
            .collect(Collectors.toList());

    // Execute pipeline
    return executePipeline(query, handler, applicableBehaviors, 0);
  }

  @SuppressWarnings("unchecked")
  private <TResponse> QueryHandler<Query<TResponse>, TResponse> findHandler(
      Query<TResponse> query) {
    if (handlerCache == null) {
      synchronized (this) {
        if (handlerCache == null) {
          handlerCache =
              queryHandlers.stream()
                  .collect(Collectors.toMap(QueryHandler::getQueryType, Function.identity()));
        }
      }
    }

    QueryHandler<?, ?> handler = handlerCache.get(query.getClass());
    if (handler == null) {
      throw new BusinessException(
          ErrorCode.SYSTEM_CONFIGURATION_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    return (QueryHandler<Query<TResponse>, TResponse>) handler;
  }

  private <TResponse> TResponse executePipeline(
      Query<TResponse> query,
      QueryHandler<Query<TResponse>, TResponse> handler,
      List<PipelineBehavior<Query<TResponse>, TResponse>> behaviors,
      int currentIndex) {

    if (currentIndex >= behaviors.size()) {
      // End of pipeline, execute the actual handler
      return handler.handle(query);
    }

    PipelineBehavior<Query<TResponse>, TResponse> currentBehavior = behaviors.get(currentIndex);

    return currentBehavior.handle(
        query, () -> executePipeline(query, handler, behaviors, currentIndex + 1));
  }
}
