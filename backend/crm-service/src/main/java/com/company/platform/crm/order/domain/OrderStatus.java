package com.company.platform.crm.order.domain;

/** Domain value object for order status. */
public enum OrderStatus {
  PENDING, // Order created but not yet confirmed
  CONFIRMED, // Order confirmed and ready for processing
  COMPLETED, // Order has been fulfilled
  CANCELLED // Order has been cancelled
}
