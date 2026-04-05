package com.company.platform.crm.order.infrastructure.persistence;

import com.company.platform.crm.order.domain.Order;
import com.company.platform.crm.order.domain.OrderItem;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

  public OrderJpaEntity toJpaEntity(Order order) {
    if (order == null) {
      return null;
    }

    OrderJpaEntity entity =
        OrderJpaEntity.builder()
            .orderNumber(order.getOrderNumber())
            .customerId(
                order.getCustomerId() != null ? UUID.fromString(order.getCustomerId()) : null)
            .status(order.getStatus())
            .totalAmount(order.getTotalAmount())
            .currency(order.getCurrency())
            .orderDate(order.getOrderDate())
            .notes(order.getNotes())
            .build();

    if (order.getId() != null) {
      entity.setId(UUID.fromString(order.getId()));
    }
    if (order.getCreatedAt() != null) {
      entity.setCreatedAt(order.getCreatedAt());
    }
    if (order.getUpdatedAt() != null) {
      entity.setUpdatedAt(order.getUpdatedAt());
    }

    if (order.getItems() != null) {
      List<OrderItemJpaEntity> itemEntities =
          order.getItems().stream().map(item -> toItemJpaEntity(item, entity)).toList();
      itemEntities.forEach(entity::addItem);
    }

    return entity;
  }

  public Order toDomainEntity(OrderJpaEntity entity) {
    if (entity == null) {
      return null;
    }

    List<OrderItem> items =
        entity.getItems() != null
            ? entity.getItems().stream().map(this::toItemDomainEntity).toList()
            : List.of();

    return Order.builder()
        .id(entity.getId() != null ? entity.getId().toString() : null)
        .orderNumber(entity.getOrderNumber())
        .customerId(entity.getCustomerId() != null ? entity.getCustomerId().toString() : null)
        .status(entity.getStatus())
        .totalAmount(entity.getTotalAmount())
        .currency(entity.getCurrency())
        .orderDate(entity.getOrderDate())
        .notes(entity.getNotes())
        .items(items)
        .createdBy(entity.getCreatedBy() != null ? entity.getCreatedBy().toString() : null)
        .createdAt(entity.getCreatedAt())
        .updatedBy(entity.getUpdatedBy() != null ? entity.getUpdatedBy().toString() : null)
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  private OrderItemJpaEntity toItemJpaEntity(OrderItem item, OrderJpaEntity orderEntity) {
    OrderItemJpaEntity itemEntity =
        OrderItemJpaEntity.builder()
            .order(orderEntity)
            .productName(item.getProductName())
            .quantity(item.getQuantity())
            .unitPrice(item.getUnitPrice())
            .totalPrice(item.getTotalPrice())
            .build();

    if (item.getId() != null) {
      itemEntity.setId(UUID.fromString(item.getId()));
    }

    return itemEntity;
  }

  private OrderItem toItemDomainEntity(OrderItemJpaEntity entity) {
    return OrderItem.builder()
        .id(entity.getId() != null ? entity.getId().toString() : null)
        .productName(entity.getProductName())
        .quantity(entity.getQuantity())
        .unitPrice(entity.getUnitPrice())
        .totalPrice(entity.getTotalPrice())
        .build();
  }
}
