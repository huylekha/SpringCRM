package com.company.platform.crm.order.application.command;

import com.company.platform.crm.infrastructure.messaging.outbox.OutboxService;
import com.company.platform.crm.order.domain.Order;
import com.company.platform.crm.order.domain.OrderItem;
import com.company.platform.crm.order.domain.OrderRepository;
import com.company.platform.crm.order.domain.event.OrderCreatedEvent;
import com.company.platform.shared.cqrs.CommandHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Handler for CreateOrderCommand. Creates a new order and publishes OrderCreatedEvent via outbox.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreateOrderCommandHandler
    implements CommandHandler<CreateOrderCommand, CreateOrderResponse> {

  private final OrderRepository orderRepository;
  private final OutboxService outboxService;

  @Override
  public CreateOrderResponse handle(CreateOrderCommand command) {
    log.info("Creating order for customer: {}", command.getCustomerId());

    // Convert command items to domain items
    List<OrderItem> orderItems =
        command.getItems().stream()
            .map(
                item ->
                    OrderItem.create(
                        item.getProductName(), item.getQuantity(), item.getUnitPrice()))
            .toList();

    // Create domain order
    String createdBy = getCurrentUserId();
    Order order =
        Order.create(
            command.getCustomerId(),
            command.getCurrency(),
            command.getNotes(),
            orderItems,
            createdBy);

    // Save order (this happens within the transaction)
    Order savedOrder = orderRepository.save(order);

    // Store domain event in outbox (same transaction)
    OrderCreatedEvent event =
        OrderCreatedEvent.from(
            savedOrder.getId(),
            savedOrder.getOrderNumber(),
            savedOrder.getCustomerId(),
            savedOrder.getTotalAmount(),
            savedOrder.getCurrency(),
            savedOrder.getOrderDate(),
            savedOrder.getCreatedBy());

    outboxService.storeEvent("Order", savedOrder.getId(), "OrderCreatedEvent", event);

    log.info(
        "Order created successfully: orderId={}, orderNumber={}",
        savedOrder.getId(),
        savedOrder.getOrderNumber());

    return CreateOrderResponse.builder()
        .orderId(savedOrder.getId())
        .orderNumber(savedOrder.getOrderNumber())
        .customerId(savedOrder.getCustomerId())
        .status(savedOrder.getStatus())
        .totalAmount(savedOrder.getTotalAmount())
        .currency(savedOrder.getCurrency())
        .orderDate(savedOrder.getOrderDate())
        .createdAt(savedOrder.getCreatedAt())
        .createdBy(savedOrder.getCreatedBy())
        .build();
  }

  @Override
  public Class<CreateOrderCommand> getCommandType() {
    return CreateOrderCommand.class;
  }

  private String getCurrentUserId() {
    try {
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.isAuthenticated()) {
        return authentication.getName();
      }
    } catch (Exception e) {
      log.debug("Could not get current user ID: {}", e.getMessage());
    }
    return "system";
  }
}
