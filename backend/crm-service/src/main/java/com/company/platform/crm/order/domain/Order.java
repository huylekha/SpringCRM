package com.company.platform.crm.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

/** Domain entity for Order (persistence-ignorant). Contains business logic and domain rules. */
@Getter
@Builder(toBuilder = true)
@With
public class Order {

  private final String id;
  private final String orderNumber;
  private final String customerId;
  private final OrderStatus status;
  private final BigDecimal totalAmount;
  private final String currency;
  private final Instant orderDate;
  private final String notes;
  private final List<OrderItem> items;
  private final String createdBy;
  private final Instant createdAt;
  private final String updatedBy;
  private final Instant updatedAt;

  /** Create a new order with generated ID and order number. */
  public static Order create(
      String customerId, String currency, String notes, List<OrderItem> items, String createdBy) {
    String orderId = UUID.randomUUID().toString();
    String orderNumber = generateOrderNumber();
    BigDecimal totalAmount = calculateTotalAmount(items);
    Instant now = Instant.now();

    return Order.builder()
        .id(orderId)
        .orderNumber(orderNumber)
        .customerId(customerId)
        .status(OrderStatus.PENDING)
        .totalAmount(totalAmount)
        .currency(currency)
        .orderDate(now)
        .notes(notes)
        .items(new ArrayList<>(items))
        .createdBy(createdBy)
        .createdAt(now)
        .build();
  }

  /** Confirm the order (business rule: can only confirm pending orders). */
  public Order confirm(String updatedBy) {
    if (this.status != OrderStatus.PENDING) {
      throw new IllegalStateException("Can only confirm pending orders");
    }

    return this.toBuilder()
        .status(OrderStatus.CONFIRMED)
        .updatedBy(updatedBy)
        .updatedAt(Instant.now())
        .build();
  }

  /** Cancel the order (business rule: cannot cancel completed orders). */
  public Order cancel(String updatedBy) {
    if (this.status == OrderStatus.COMPLETED) {
      throw new IllegalStateException("Cannot cancel completed orders");
    }

    return this.toBuilder()
        .status(OrderStatus.CANCELLED)
        .updatedBy(updatedBy)
        .updatedAt(Instant.now())
        .build();
  }

  /** Complete the order (business rule: must be confirmed first). */
  public Order complete(String updatedBy) {
    if (this.status != OrderStatus.CONFIRMED) {
      throw new IllegalStateException("Can only complete confirmed orders");
    }

    return this.toBuilder()
        .status(OrderStatus.COMPLETED)
        .updatedBy(updatedBy)
        .updatedAt(Instant.now())
        .build();
  }

  private static String generateOrderNumber() {
    // Simple order number generation - in real system this would be more sophisticated
    return "ORD-" + System.currentTimeMillis();
  }

  private static BigDecimal calculateTotalAmount(List<OrderItem> items) {
    return items.stream().map(OrderItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
  }
}
