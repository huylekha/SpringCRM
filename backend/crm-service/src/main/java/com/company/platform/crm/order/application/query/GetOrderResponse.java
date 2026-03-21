package com.company.platform.crm.order.application.query;

import com.company.platform.crm.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Response from getting an order. */
@Getter
@Builder
public class GetOrderResponse {

  private final String orderId;
  private final String orderNumber;
  private final String customerId;
  private final OrderStatus status;
  private final BigDecimal totalAmount;
  private final String currency;
  private final Instant orderDate;
  private final String notes;
  private final List<OrderItemResponse> items;
  private final Instant createdAt;
  private final String createdBy;
  private final Instant updatedAt;
  private final String updatedBy;

  @Getter
  @Builder
  public static class OrderItemResponse {
    private final String itemId;
    private final String productName;
    private final Integer quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal totalPrice;
  }
}
