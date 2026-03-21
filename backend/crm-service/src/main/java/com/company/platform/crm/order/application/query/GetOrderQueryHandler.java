package com.company.platform.crm.order.application.query;

import com.company.platform.crm.order.domain.Order;
import com.company.platform.crm.order.domain.OrderRepository;
import com.company.platform.shared.cqrs.QueryHandler;
import com.company.platform.shared.exception.BusinessException;
import com.company.platform.shared.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Handler for GetOrderQuery. Retrieves order information (read-only operation). */
@Component
@RequiredArgsConstructor
@Slf4j
public class GetOrderQueryHandler implements QueryHandler<GetOrderQuery, GetOrderResponse> {

  private final OrderRepository orderRepository;

  @Override
  @Transactional(readOnly = true)
  public GetOrderResponse handle(GetOrderQuery query) {
    log.debug("Getting order: orderId={}", query.getOrderId());

    Order order =
        orderRepository
            .findById(query.getOrderId())
            .orElseThrow(
                () -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, HttpStatus.NOT_FOUND));

    List<GetOrderResponse.OrderItemResponse> itemResponses =
        order.getItems().stream()
            .map(
                item ->
                    GetOrderResponse.OrderItemResponse.builder()
                        .itemId(item.getId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .totalPrice(item.getTotalPrice())
                        .build())
            .toList();

    return GetOrderResponse.builder()
        .orderId(order.getId())
        .orderNumber(order.getOrderNumber())
        .customerId(order.getCustomerId())
        .status(order.getStatus())
        .totalAmount(order.getTotalAmount())
        .currency(order.getCurrency())
        .orderDate(order.getOrderDate())
        .notes(order.getNotes())
        .items(itemResponses)
        .createdAt(order.getCreatedAt())
        .createdBy(order.getCreatedBy())
        .updatedAt(order.getUpdatedAt())
        .updatedBy(order.getUpdatedBy())
        .build();
  }

  @Override
  public Class<GetOrderQuery> getQueryType() {
    return GetOrderQuery.class;
  }
}
