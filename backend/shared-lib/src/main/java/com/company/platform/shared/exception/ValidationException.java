package com.company.platform.shared.exception;

import java.util.List;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Exception thrown when validation fails. Extends BusinessException to include field-level
 * validation errors.
 */
@Getter
public class ValidationException extends BusinessException {

  private static final long serialVersionUID = 1L;

  private final List<FieldError> fieldErrors;

  public ValidationException(List<FieldError> fieldErrors) {
    super(ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
    this.fieldErrors = fieldErrors != null ? List.copyOf(fieldErrors) : List.of();
  }

  public ValidationException(ErrorCode errorCode, List<FieldError> fieldErrors) {
    super(errorCode, HttpStatus.BAD_REQUEST);
    this.fieldErrors = fieldErrors != null ? List.copyOf(fieldErrors) : List.of();
  }

  public List<FieldError> getFieldErrors() {
    return List.copyOf(fieldErrors);
  }
}
