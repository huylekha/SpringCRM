package com.company.platform.crm.infrastructure.messaging;

import com.company.platform.crm.infrastructure.messaging.kafka.KafkaTopics;
import com.company.platform.shared.messaging.outbox.EventTopicMapper;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * CRM-specific implementation of EventTopicMapper. Maps CRM domain events to their corresponding
 * Kafka topics.
 */
@Component
public class CrmEventTopicMapper implements EventTopicMapper {

  private static final Map<String, String> EVENT_TYPE_TO_TOPIC =
      Map.of(
          "OrderCreatedEvent", KafkaTopics.ORDER_CREATED,
          "OrderUpdatedEvent", KafkaTopics.ORDER_UPDATED);

  @Override
  public Optional<String> getTopicForEventType(String eventType) {
    return Optional.ofNullable(EVENT_TYPE_TO_TOPIC.get(eventType));
  }
}
