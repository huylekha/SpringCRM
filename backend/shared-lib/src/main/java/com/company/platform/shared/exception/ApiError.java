package com.company.platform.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

  private final String code;
  private final String message;
  private final String traceId;
  private final String path; // Request URI for debugging
  private final String method; // HTTP method (GET, POST, etc.)
  @Builder.Default private final Instant timestamp = Instant.now();
  private final List<FieldError> details;
}
