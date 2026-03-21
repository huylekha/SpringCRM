package com.company.platform.crm.order.domain.event;

import com.company.platform.crm.order.domain.OrderStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/** Domain event published when an order is updated. */
@Getter
@Builder
public class OrderUpdatedEvent {

  private final String eventId;
  private final Integer eventVersion;
  private final Instant occurredAt;
  private final String orderId;
  private final String orderNumber;
  private final OrderStatus oldStatus;
  private final OrderStatus newStatus;
  private final String updatedBy;

  public static OrderUpdatedEvent from(
      String orderId,
      String orderNumber,
      OrderStatus oldStatus,
      OrderStatus newStatus,
      String updatedBy) {
    return OrderUpdatedEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventVersion(1)
        .occurredAt(Instant.now())
        .orderId(orderId)
        .orderNumber(orderNumber)
        .oldStatus(oldStatus)
        .newStatus(newStatus)
        .updatedBy(updatedBy)
        .build();
  }
}
