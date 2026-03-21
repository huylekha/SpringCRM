package com.company.platform.crm.order.application.command;

import com.company.platform.shared.cqrs.Command;
import com.company.platform.shared.cqrs.Idempotent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Command to create a new order. Marked as idempotent to prevent duplicate order creation. */
@Getter
@Builder
@Idempotent(ttlSeconds = 3600) // 1 hour idempotency window
public class CreateOrderCommand implements Command<CreateOrderResponse> {

  @NotBlank(message = "VALIDATION_FIELD_REQUIRED")
  private final String customerId;

  @NotBlank(message = "VALIDATION_FIELD_REQUIRED")
  private final String currency;

  private final String notes;

  @NotEmpty(message = "VALIDATION_LIST_NOT_EMPTY")
  @Valid
  private final List<CreateOrderItemRequest> items;

  @Getter
  @Builder
  public static class CreateOrderItemRequest {

    @NotBlank(message = "VALIDATION_FIELD_REQUIRED")
    private final String productName;

    @NotNull(message = "VALIDATION_FIELD_REQUIRED")
    private final Integer quantity;

    @NotNull(message = "VALIDATION_FIELD_REQUIRED")
    private final java.math.BigDecimal unitPrice;
  }
}
