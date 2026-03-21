package com.company.platform.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

  private final String code;
  private final String message;
  private final String traceId;
  private final String path; // Request URI for debugging
  private final String method; // HTTP method (GET, POST, etc.)
  @Builder.Default private final Instant timestamp = Instant.now();
  private final List<FieldError> details;

  public ApiError(
      String code,
      String message,
      String traceId,
      String path,
      String method,
      Instant timestamp,
      List<FieldError> details) {
    this.code = code;
    this.message = message;
    this.traceId = traceId;
    this.path = path;
    this.method = method;
    this.timestamp = timestamp != null ? timestamp : Instant.now();
    this.details = details != null ? List.copyOf(details) : null;
  }

  public List<FieldError> getDetails() {
    return details != null ? List.copyOf(details) : null;
  }
}
