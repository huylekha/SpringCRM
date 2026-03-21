package com.company.platform.crm.order.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.company.platform.crm.order.domain.Order;
import com.company.platform.crm.order.domain.OrderRepository;
import com.company.platform.crm.order.domain.OrderStatus;
import com.company.platform.shared.messaging.outbox.OutboxService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class CreateOrderCommandHandlerTest {

  @Mock private OrderRepository orderRepository;

  @Mock private OutboxService outboxService;

  @Mock private SecurityContext securityContext;

  @Mock private Authentication authentication;

  private CreateOrderCommandHandler handler;

  @BeforeEach
  void setUp() {
    handler = new CreateOrderCommandHandler(orderRepository, outboxService);

    // Mock security context
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getName()).thenReturn("test-user");
  }

  @Test
  void shouldCreateOrderSuccessfully() {
    // Given
    CreateOrderCommand command =
        CreateOrderCommand.builder()
            .customerId("customer-123")
            .currency("USD")
            .notes("Test order")
            .items(
                List.of(
                    CreateOrderCommand.CreateOrderItemRequest.builder()
                        .productName("Product A")
                        .quantity(2)
                        .unitPrice(new BigDecimal("10.00"))
                        .build()))
            .build();

    Order savedOrder =
        Order.builder()
            .id("order-123")
            .orderNumber("ORD-123")
            .customerId("customer-123")
            .status(OrderStatus.PENDING)
            .totalAmount(new BigDecimal("20.00"))
            .currency("USD")
            .orderDate(Instant.now())
            .createdBy("test-user")
            .createdAt(Instant.now())
            .build();

    when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

    // When
    CreateOrderResponse response = handler.handle(command);

    // Then
    assertThat(response.getOrderId()).isEqualTo("order-123");
    assertThat(response.getOrderNumber()).isEqualTo("ORD-123");
    assertThat(response.getCustomerId()).isEqualTo("customer-123");
    assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(response.getTotalAmount()).isEqualTo(new BigDecimal("20.00"));
    assertThat(response.getCurrency()).isEqualTo("USD");
    assertThat(response.getCreatedBy()).isEqualTo("test-user");

    // Verify order was saved
    ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
    verify(orderRepository).save(orderCaptor.capture());
    Order capturedOrder = orderCaptor.getValue();
    assertThat(capturedOrder.getCustomerId()).isEqualTo("customer-123");
    assertThat(capturedOrder.getItems()).hasSize(1);

    // Verify outbox event was stored
    verify(outboxService).storeEvent(eq("Order"), eq("order-123"), eq("OrderCreatedEvent"), any());
  }

  @Test
  void shouldCalculateTotalAmountCorrectly() {
    // Given
    CreateOrderCommand command =
        CreateOrderCommand.builder()
            .customerId("customer-123")
            .currency("USD")
            .items(
                List.of(
                    CreateOrderCommand.CreateOrderItemRequest.builder()
                        .productName("Product A")
                        .quantity(2)
                        .unitPrice(new BigDecimal("10.50"))
                        .build(),
                    CreateOrderCommand.CreateOrderItemRequest.builder()
                        .productName("Product B")
                        .quantity(1)
                        .unitPrice(new BigDecimal("5.25"))
                        .build()))
            .build();

    when(orderRepository.save(any(Order.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    // When
    handler.handle(command);

    // Then
    ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
    verify(orderRepository).save(orderCaptor.capture());
    Order capturedOrder = orderCaptor.getValue();

    // Total should be (2 * 10.50) + (1 * 5.25) = 26.25
    assertThat(capturedOrder.getTotalAmount()).isEqualTo(new BigDecimal("26.25"));
  }
}
