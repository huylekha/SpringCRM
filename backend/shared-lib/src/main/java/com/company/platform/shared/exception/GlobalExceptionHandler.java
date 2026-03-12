package com.company.platform.shared.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                      HttpServletRequest request) {
        List<FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldError(e.getField(), e.getDefaultMessage()))
                .toList();
        ApiError error = ApiError.builder()
                .code("AUTH_VALIDATION_FAILED")
                .message("Request validation failed")
                .details(details)
                .traceId(request.getHeader("X-Correlation-Id"))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex,
                                                    HttpServletRequest request) {
        ApiError error = ApiError.builder()
                .code(ex.getCode())
                .message(ex.getMessage())
                .traceId(request.getHeader("X-Correlation-Id"))
                .build();
        return ResponseEntity.status(ex.getStatus()).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                                                        HttpServletRequest request) {
        ApiError error = ApiError.builder()
                .code("AUTH_INSUFFICIENT_PERMISSION")
                .message("You do not have permission to perform this action")
                .traceId(request.getHeader("X-Correlation-Id"))
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex,
                                                   HttpServletRequest request) {
        ApiError error = ApiError.builder()
                .code("SYSTEM_INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .traceId(request.getHeader("X-Correlation-Id"))
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
