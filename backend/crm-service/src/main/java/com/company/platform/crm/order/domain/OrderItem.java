package com.company.platform.crm.order.domain;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

/** Domain value object for order items. */
@Getter
@Builder(toBuilder = true)
@With
public class OrderItem {

  private final String id;
  private final String productName;
  private final Integer quantity;
  private final BigDecimal unitPrice;
  private final BigDecimal totalPrice;

  /** Create a new order item with calculated total price. */
  public static OrderItem create(String productName, Integer quantity, BigDecimal unitPrice) {
    if (quantity <= 0) {
      throw new IllegalArgumentException("Quantity must be positive");
    }
    if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Unit price cannot be negative");
    }

    String itemId = UUID.randomUUID().toString();
    BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

    return OrderItem.builder()
        .id(itemId)
        .productName(productName)
        .quantity(quantity)
        .unitPrice(unitPrice)
        .totalPrice(totalPrice)
        .build();
  }
}
