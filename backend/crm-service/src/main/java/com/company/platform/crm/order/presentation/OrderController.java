package com.company.platform.crm.order.presentation;

import com.company.platform.crm.order.application.command.CreateOrderCommand;
import com.company.platform.crm.order.application.command.CreateOrderResponse;
import com.company.platform.crm.order.application.query.GetOrderQuery;
import com.company.platform.crm.order.application.query.GetOrderResponse;
import com.company.platform.shared.cqrs.CommandBus;
import com.company.platform.shared.cqrs.QueryBus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** REST controller for Order operations. Delegates to CQRS command and query buses. */
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  /** Create a new order. */
  @PostMapping
  public ResponseEntity<CreateOrderResponse> createOrder(
      @Valid @RequestBody CreateOrderCommand command) {
    log.info("Creating order for customer: {}", command.getCustomerId());

    CreateOrderResponse response = commandBus.send(command);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  /** Get an order by ID. */
  @GetMapping("/{orderId}")
  public ResponseEntity<GetOrderResponse> getOrder(@PathVariable String orderId) {
    log.info("Getting order: {}", orderId);

    GetOrderQuery query = GetOrderQuery.builder().orderId(orderId).build();

    GetOrderResponse response = queryBus.send(query);

    return ResponseEntity.ok(response);
  }
}
