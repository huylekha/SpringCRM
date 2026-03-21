package com.company.platform.shared.cqrs.behavior;

import com.company.platform.shared.cqrs.Command;
import com.company.platform.shared.cqrs.PipelineBehavior;
import com.company.platform.shared.cqrs.RequestHandlerDelegate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pipeline behavior that wraps command processing in a database transaction. Only applies to
 * commands (write operations), not queries. Uses Spring's @Transactional annotation for transaction
 * management.
 */
@Component
@Slf4j
public class TransactionBehavior<TRequest, TResponse>
    implements PipelineBehavior<TRequest, TResponse> {

  @Override
  @Transactional
  public TResponse handle(TRequest request, RequestHandlerDelegate<TResponse> next) {
    log.debug("Executing request in transaction: {}", request.getClass().getSimpleName());
    return next.handle();
  }

  @Override
  public int getOrder() {
    return 300; // Execute after validation and idempotency, before logging
  }

  @Override
  public boolean canHandle(Class<?> requestType) {
    // Only apply transactions to commands, not queries
    return Command.class.isAssignableFrom(requestType);
  }
}
