package com.company.platform.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {

  private final ErrorCode errorCode;
  private final HttpStatus status;

  public BusinessException(ErrorCode errorCode, HttpStatus status) {
    super(errorCode.getDefaultMessage());
    this.errorCode = errorCode;
    this.status = status;
  }

  public BusinessException(ErrorCode errorCode) {
    this(errorCode, HttpStatus.BAD_REQUEST);
  }

  // Legacy constructor for backward compatibility
  @Deprecated
  public BusinessException(String code, String message, HttpStatus status) {
    super(message);
    this.errorCode = null;
    this.status = status;
  }

  public String getCode() {
    return errorCode != null ? errorCode.getCode() : null;
  }
}
