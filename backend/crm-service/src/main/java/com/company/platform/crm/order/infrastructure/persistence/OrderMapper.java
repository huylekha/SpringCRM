package com.company.platform.crm.order.infrastructure.persistence;

import com.company.platform.crm.order.domain.Order;
import com.company.platform.crm.order.domain.OrderItem;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Mapper between domain Order and JPA OrderJpaEntity. Handles the conversion between
 * persistence-ignorant domain objects and JPA entities.
 */
@Component
public class OrderMapper {

  /** Convert domain Order to JPA entity. */
  public OrderJpaEntity toJpaEntity(Order order) {
    if (order == null) {
      return null;
    }

    OrderJpaEntity entity =
        OrderJpaEntity.builder()
            .id(order.getId())
            .orderNumber(order.getOrderNumber())
            .customerId(order.getCustomerId())
            .status(order.getStatus())
            .totalAmount(order.getTotalAmount())
            .currency(order.getCurrency())
            .orderDate(order.getOrderDate())
            .notes(order.getNotes())
            .createdBy(order.getCreatedBy())
            .updatedBy(order.getUpdatedBy())
            .build();

    // Set audit fields manually since they might come from domain
    if (order.getCreatedAt() != null) {
      entity.setCreatedAt(order.getCreatedAt());
    }
    if (order.getUpdatedAt() != null) {
      entity.setUpdatedAt(order.getUpdatedAt());
    }

    // Convert items
    if (order.getItems() != null) {
      List<OrderItemJpaEntity> itemEntities =
          order.getItems().stream().map(item -> toItemJpaEntity(item, entity)).toList();

      itemEntities.forEach(entity::addItem);
    }

    return entity;
  }

  /** Convert JPA entity to domain Order. */
  public Order toDomainEntity(OrderJpaEntity entity) {
    if (entity == null) {
      return null;
    }

    List<OrderItem> items =
        entity.getItems() != null
            ? entity.getItems().stream().map(this::toItemDomainEntity).toList()
            : List.of();

    return Order.builder()
        .id(entity.getId())
        .orderNumber(entity.getOrderNumber())
        .customerId(entity.getCustomerId())
        .status(entity.getStatus())
        .totalAmount(entity.getTotalAmount())
        .currency(entity.getCurrency())
        .orderDate(entity.getOrderDate())
        .notes(entity.getNotes())
        .items(items)
        .createdBy(entity.getCreatedBy())
        .createdAt(entity.getCreatedAt())
        .updatedBy(entity.getUpdatedBy())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  /** Convert domain OrderItem to JPA entity. */
  private OrderItemJpaEntity toItemJpaEntity(OrderItem item, OrderJpaEntity orderEntity) {
    return OrderItemJpaEntity.builder()
        .id(item.getId())
        .order(orderEntity)
        .productName(item.getProductName())
        .quantity(item.getQuantity())
        .unitPrice(item.getUnitPrice())
        .totalPrice(item.getTotalPrice())
        .build();
  }

  /** Convert JPA entity to domain OrderItem. */
  private OrderItem toItemDomainEntity(OrderItemJpaEntity entity) {
    return OrderItem.builder()
        .id(entity.getId())
        .productName(entity.getProductName())
        .quantity(entity.getQuantity())
        .unitPrice(entity.getUnitPrice())
        .totalPrice(entity.getTotalPrice())
        .build();
  }
}
