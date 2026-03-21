package com.company.platform.crm.order.application.command;

import com.company.platform.crm.order.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/** Response from creating an order. */
@Getter
@Builder
public class CreateOrderResponse {

  private final String orderId;
  private final String orderNumber;
  private final String customerId;
  private final OrderStatus status;
  private final BigDecimal totalAmount;
  private final String currency;
  private final Instant orderDate;
  private final Instant createdAt;
  private final String createdBy;
}
