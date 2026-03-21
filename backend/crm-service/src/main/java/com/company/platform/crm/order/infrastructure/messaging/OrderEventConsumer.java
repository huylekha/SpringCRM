package com.company.platform.crm.order.infrastructure.messaging;

import com.company.platform.crm.infrastructure.messaging.inbox.InboxService;
import com.company.platform.crm.order.domain.event.OrderCreatedEvent;
import com.company.platform.crm.order.domain.event.OrderUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for order events. Demonstrates the inbox pattern for idempotent event processing.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

  private final InboxService inboxService;

  /**
   * Handle OrderCreatedEvent from Kafka. In a real system, this might trigger workflows like: -
   * Sending confirmation emails - Updating inventory - Creating shipping labels
   */
  @KafkaListener(topics = "crm.order.created", groupId = "${spring.application.name}")
  public void handleOrderCreated(
      @Payload OrderCreatedEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

    String messageId = generateMessageId(event.getEventId(), topic);

    // Try to process the message (inbox pattern for deduplication)
    boolean isFirstTime =
        inboxService.tryProcessMessage(messageId, "OrderCreatedEvent", "crm-service");

    if (!isFirstTime) {
      log.debug(
          "Duplicate OrderCreatedEvent ignored: eventId={}, orderId={}",
          event.getEventId(),
          event.getOrderId());
      return;
    }

    log.info(
        "Processing OrderCreatedEvent: eventId={}, orderId={}, orderNumber={}, totalAmount={}",
        event.getEventId(),
        event.getOrderId(),
        event.getOrderNumber(),
        event.getTotalAmount());

    // Simulate business logic
    try {
      // In a real system, this would:
      // 1. Send order confirmation email to customer
      // 2. Update inventory levels
      // 3. Create shipping workflow
      // 4. Update analytics/reporting systems

      log.info("Order created event processed successfully: orderId={}", event.getOrderId());

    } catch (Exception e) {
      log.error(
          "Failed to process OrderCreatedEvent: eventId={}, orderId={}",
          event.getEventId(),
          event.getOrderId(),
          e);
      throw e; // This will trigger retry/DLQ handling
    }
  }

  /** Handle OrderUpdatedEvent from Kafka. */
  @KafkaListener(topics = "crm.order.updated", groupId = "${spring.application.name}")
  public void handleOrderUpdated(
      @Payload OrderUpdatedEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

    String messageId = generateMessageId(event.getEventId(), topic);

    boolean isFirstTime =
        inboxService.tryProcessMessage(messageId, "OrderUpdatedEvent", "crm-service");

    if (!isFirstTime) {
      log.debug(
          "Duplicate OrderUpdatedEvent ignored: eventId={}, orderId={}",
          event.getEventId(),
          event.getOrderId());
      return;
    }

    log.info(
        "Processing OrderUpdatedEvent: eventId={}, orderId={}, oldStatus={}, newStatus={}",
        event.getEventId(),
        event.getOrderId(),
        event.getOldStatus(),
        event.getNewStatus());

    try {
      // Business logic for order updates
      // - Notify customer of status changes
      // - Update external systems
      // - Trigger additional workflows based on status

      log.info("Order updated event processed successfully: orderId={}", event.getOrderId());

    } catch (Exception e) {
      log.error(
          "Failed to process OrderUpdatedEvent: eventId={}, orderId={}",
          event.getEventId(),
          event.getOrderId(),
          e);
      throw e;
    }
  }

  /**
   * Generate a unique message ID for inbox deduplication. Combines event ID with topic to ensure
   * uniqueness.
   */
  private String generateMessageId(String eventId, String topic) {
    return String.format("%s:%s", eventId, topic != null ? topic : "no-topic");
  }
}
