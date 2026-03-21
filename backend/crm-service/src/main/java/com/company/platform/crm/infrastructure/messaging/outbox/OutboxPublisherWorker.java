package com.company.platform.crm.infrastructure.messaging.outbox;

import com.company.platform.crm.infrastructure.messaging.kafka.KafkaTopics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Background worker that polls the outbox table and publishes pending messages to Kafka. Runs on a
 * scheduled basis to ensure reliable event delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisherWorker {

  private final OutboxMessageRepository outboxRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final ObjectMapper objectMapper;

  private static final int BATCH_SIZE = 50;
  private static final Map<String, String> EVENT_TYPE_TO_TOPIC =
      Map.of(
          "OrderCreatedEvent", KafkaTopics.ORDER_CREATED,
          "OrderUpdatedEvent", KafkaTopics.ORDER_UPDATED);

  /** Process pending outbox messages every 5 seconds. */
  @Scheduled(fixedDelay = 5000)
  public void processOutboxMessages() {
    List<OutboxMessage> pendingMessages =
        outboxRepository.findPendingMessages(PageRequest.of(0, BATCH_SIZE));

    if (pendingMessages.isEmpty()) {
      return;
    }

    log.info("Processing {} pending outbox messages", pendingMessages.size());

    for (OutboxMessage message : pendingMessages) {
      try {
        publishMessage(message);
      } catch (Exception e) {
        log.error(
            "Failed to process outbox message: id={}, eventType={}",
            message.getId(),
            message.getEventType(),
            e);
        handlePublishFailure(message);
      }
    }
  }

  private void publishMessage(OutboxMessage message) {
    String topic = EVENT_TYPE_TO_TOPIC.get(message.getEventType());
    if (topic == null) {
      log.warn("No topic mapping found for event type: {}", message.getEventType());
      handlePublishFailure(message);
      return;
    }

    try {
      // Parse the event data back to an object
      Object eventData = objectMapper.readValue(message.getEventData(), Object.class);

      // Use aggregate ID as Kafka key for partitioning
      String key = message.getAggregateId();

      CompletableFuture<SendResult<String, Object>> future =
          kafkaTemplate.send(topic, key, eventData);

      future.whenComplete(
          (result, ex) -> {
            if (ex == null) {
              handlePublishSuccess(message, result);
            } else {
              log.error(
                  "Failed to send message to Kafka: messageId={}, topic={}",
                  message.getId(),
                  topic,
                  ex);
              handlePublishFailure(message);
            }
          });

    } catch (JsonProcessingException e) {
      log.error("Failed to parse event data: messageId={}", message.getId(), e);
      handlePublishFailure(message);
    }
  }

  @Transactional
  protected void handlePublishSuccess(OutboxMessage message, SendResult<String, Object> result) {
    message.markAsSent();
    outboxRepository.save(message);

    log.debug(
        "Successfully published message: id={}, topic={}, partition={}, offset={}",
        message.getId(),
        result.getRecordMetadata().topic(),
        result.getRecordMetadata().partition(),
        result.getRecordMetadata().offset());
  }

  @Transactional
  protected void handlePublishFailure(OutboxMessage message) {
    message.markAsFailed();
    outboxRepository.save(message);

    if (message.getStatus() == OutboxStatus.FAILED) {
      log.error(
          "Message failed permanently after {} retries: id={}, eventType={}",
          message.getRetryCount(),
          message.getId(),
          message.getEventType());
    } else {
      log.warn(
          "Message failed, will retry: id={}, eventType={}, retryCount={}",
          message.getId(),
          message.getEventType(),
          message.getRetryCount());
    }
  }

  /** Cleanup old processed messages every hour. */
  @Scheduled(fixedDelay = 3600000) // 1 hour
  @Transactional
  public void cleanupOldMessages() {
    // Keep processed messages for 7 days
    var cutoffTime = java.time.Instant.now().minusSeconds(7 * 24 * 3600);

    List<OutboxMessage> oldMessages =
        outboxRepository.findOldProcessedMessages(cutoffTime, PageRequest.of(0, 1000));

    if (!oldMessages.isEmpty()) {
      outboxRepository.deleteAll(oldMessages);
      log.info("Cleaned up {} old outbox messages", oldMessages.size());
    }
  }
}
