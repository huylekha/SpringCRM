package com.company.platform.shared.messaging.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing outbox messages. Provides methods to store domain events that need to be
 * published to Kafka.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

  private final OutboxMessageRepository outboxRepository;
  private final ObjectMapper objectMapper;

  /**
   * Store a domain event in the outbox for later publishing. This method should be called within
   * the same transaction as the domain operation.
   */
  @Transactional
  public void storeEvent(
      String aggregateType, String aggregateId, String eventType, Object eventData) {
    try {
      String eventDataJson = objectMapper.writeValueAsString(eventData);

      OutboxMessage message =
          OutboxMessage.builder()
              .aggregateType(aggregateType)
              .aggregateId(aggregateId)
              .eventType(eventType)
              .eventData(eventDataJson)
              .build();

      outboxRepository.save(message);

      log.debug(
          "Stored outbox message: aggregateType={}, aggregateId={}, eventType={}",
          aggregateType,
          aggregateId,
          eventType);

    } catch (JsonProcessingException e) {
      log.error(
          "Failed to serialize event data for outbox: aggregateType={}, aggregateId={}, eventType={}",
          aggregateType,
          aggregateId,
          eventType,
          e);
      throw new RuntimeException("Failed to serialize event data", e);
    }
  }

  /** Store multiple domain events in the outbox. */
  @Transactional
  public void storeEvents(String aggregateType, String aggregateId, Object... events) {
    for (Object event : events) {
      String eventType = event.getClass().getSimpleName();
      storeEvent(aggregateType, aggregateId, eventType, event);
    }
  }
}
