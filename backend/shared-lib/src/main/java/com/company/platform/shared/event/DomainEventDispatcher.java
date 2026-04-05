package com.company.platform.shared.event;

import com.company.platform.shared.entity.BaseEntity;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DomainEventDispatcher {

  private final ApplicationEventPublisher eventPublisher;

  public void dispatchAndClear(BaseEntity<?> entity) {
    List<Object> events = entity.getDomainEvents();
    if (events.isEmpty()) {
      return;
    }

    log.debug(
        "Dispatching {} domain event(s) from {}", events.size(), entity.getClass().getSimpleName());

    for (Object event : events) {
      eventPublisher.publishEvent(event);
      log.debug("Published domain event: {}", event.getClass().getSimpleName());
    }

    entity.clearDomainEvents();
  }

  public void dispatchAndClear(Iterable<? extends BaseEntity<?>> entities) {
    for (BaseEntity<?> entity : entities) {
      dispatchAndClear(entity);
    }
  }
}
