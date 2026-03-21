package com.company.platform.shared.messaging.outbox;

import java.util.Optional;

/**
 * Interface for mapping event types to Kafka topics. Services should provide their own
 * implementation based on their domain events.
 */
public interface EventTopicMapper {

  /**
   * Map an event type to its corresponding Kafka topic.
   *
   * @param eventType the event type (usually the class simple name)
   * @return the Kafka topic name, or empty if no mapping exists
   */
  Optional<String> getTopicForEventType(String eventType);
}
