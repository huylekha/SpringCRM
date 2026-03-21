package com.company.platform.crm.order.application.query;

import com.company.platform.shared.cqrs.Query;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

/** Query to get an order by ID. */
@Getter
@Builder
public class GetOrderQuery implements Query<GetOrderResponse> {

  @NotBlank(message = "VALIDATION_FIELD_REQUIRED")
  private final String orderId;
}
