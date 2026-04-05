package com.company.platform.shared.event;

import com.company.platform.shared.messaging.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnBean(OutboxService.class)
public class DomainEventToOutboxListener {

  private final OutboxService outboxService;

  @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
  public void onDomainEvent(OutboxableEvent event) {
    log.debug(
        "Mapping domain event to outbox: type={}, aggregateId={}",
        event.getEventType(),
        event.getAggregateId());

    outboxService.storeEvent(
        event.getAggregateType(), event.getAggregateId(), event.getEventType(), event.getPayload());
  }
}
