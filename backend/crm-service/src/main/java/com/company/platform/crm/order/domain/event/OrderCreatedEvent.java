package com.company.platform.crm.order.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

/** Domain event published when an order is created. */
@Getter
@Builder
public class OrderCreatedEvent {

  private final String eventId;
  private final Integer eventVersion;
  private final Instant occurredAt;
  private final String orderId;
  private final String orderNumber;
  private final String customerId;
  private final BigDecimal totalAmount;
  private final String currency;
  private final Instant orderDate;
  private final String createdBy;

  public static OrderCreatedEvent from(
      String orderId,
      String orderNumber,
      String customerId,
      BigDecimal totalAmount,
      String currency,
      Instant orderDate,
      String createdBy) {
    return OrderCreatedEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .eventVersion(1)
        .occurredAt(Instant.now())
        .orderId(orderId)
        .orderNumber(orderNumber)
        .customerId(customerId)
        .totalAmount(totalAmount)
        .currency(currency)
        .orderDate(orderDate)
        .createdBy(createdBy)
        .build();
  }
}
