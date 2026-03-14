package com.company.platform.shared.exception;

import com.company.platform.shared.i18n.MessageService;
import com.company.platform.shared.util.TraceIdContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  private final MessageService messageService;

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    String traceId = TraceIdContext.getCurrentTraceId();
    String path = request.getRequestURI();
    String method = request.getMethod();

    List<FieldError> details =
        ex.getBindingResult().getFieldErrors().stream()
            .map(
                e ->
                    new FieldError(
                        e.getField(),
                        messageService.getMessage(
                            e.getDefaultMessage(), null, LocaleContextHolder.getLocale())))
            .toList();

    String translatedMessage = messageService.getMessage(ErrorCode.VALIDATION_FAILED);

    // Structured logging with full context
    log.warn(
        "Validation failed: traceId={}, path={}, method={}, locale={}, fieldCount={}, userId={}",
        traceId,
        path,
        method,
        LocaleContextHolder.getLocale(),
        details.size(),
        getCurrentUserId());

    ApiError error =
        ApiError.builder()
            .code(ErrorCode.VALIDATION_FAILED.getCode())
            .message(translatedMessage)
            .details(details)
            .traceId(traceId)
            .path(path)
            .method(method)
            .build();
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
  }

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiError> handleBusiness(BusinessException ex, HttpServletRequest request) {
    String traceId = TraceIdContext.getCurrentTraceId();
    String path = request.getRequestURI();
    String method = request.getMethod();
    String translatedMessage;
    String code;

    if (ex.getErrorCode() != null) {
      translatedMessage = messageService.getMessage(ex.getErrorCode());
      code = ex.getErrorCode().getCode();
    } else {
      // Legacy backward compatibility
      translatedMessage = ex.getMessage();
      code = ex.getCode();
    }

    // Structured logging with business context
    log.warn(
        "Business exception: traceId={}, errorCode={}, path={}, method={}, locale={}, status={}, userId={}",
        traceId,
        code,
        path,
        method,
        LocaleContextHolder.getLocale(),
        ex.getStatus(),
        getCurrentUserId());

    ApiError error =
        ApiError.builder()
            .code(code)
            .message(translatedMessage)
            .traceId(traceId)
            .path(path)
            .method(method)
            .build();
    return ResponseEntity.status(ex.getStatus()).body(error);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiError> handleAuthentication(
      AuthenticationException ex, HttpServletRequest request) {
    String traceId = TraceIdContext.getCurrentTraceId();
    String path = request.getRequestURI();
    String method = request.getMethod();
    String translatedMessage = messageService.getMessage(ErrorCode.AUTH_TOKEN_INVALID);

    // Authentication failures are security events - always log with context
    log.warn(
        "Authentication failed: traceId={}, path={}, method={}, locale={}, reason={}",
        traceId,
        path,
        method,
        LocaleContextHolder.getLocale(),
        ex.getMessage());

    ApiError error =
        ApiError.builder()
            .code(ErrorCode.AUTH_TOKEN_INVALID.getCode())
            .message(translatedMessage)
            .traceId(traceId)
            .path(path)
            .method(method)
            .build();
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiError> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    String traceId = TraceIdContext.getCurrentTraceId();
    String path = request.getRequestURI();
    String method = request.getMethod();
    String translatedMessage = messageService.getMessage(ErrorCode.AUTH_INSUFFICIENT_PERMISSION);

    // Authorization failures - log with user context
    log.warn(
        "Access denied: traceId={}, path={}, method={}, locale={}, userId={}",
        traceId,
        path,
        method,
        LocaleContextHolder.getLocale(),
        getCurrentUserId());

    ApiError error =
        ApiError.builder()
            .code(ErrorCode.AUTH_INSUFFICIENT_PERMISSION.getCode())
            .message(translatedMessage)
            .traceId(traceId)
            .path(path)
            .method(method)
            .build();
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
    String traceId = TraceIdContext.getCurrentTraceId();
    String path = request.getRequestURI();
    String method = request.getMethod();
    String translatedMessage = messageService.getMessage(ErrorCode.SYSTEM_INTERNAL_ERROR);

    // Critical error - log with full stack trace and context
    log.error(
        "Unexpected error: traceId={}, path={}, method={}, locale={}, errorType={}, userId={}",
        traceId,
        path,
        method,
        LocaleContextHolder.getLocale(),
        ex.getClass().getName(),
        getCurrentUserId(),
        ex);

    ApiError error =
        ApiError.builder()
            .code(ErrorCode.SYSTEM_INTERNAL_ERROR.getCode())
            .message(translatedMessage)
            .traceId(traceId)
            .path(path)
            .method(method)
            .build();
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
  }

  /**
   * Get current user ID from SecurityContext for logging. Returns "anonymous" if not authenticated.
   */
  private String getCurrentUserId() {
    try {
      var authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null
          && authentication.isAuthenticated()
          && !"anonymousUser".equals(authentication.getPrincipal())) {
        return authentication.getName();
      }
    } catch (Exception e) {
      // Ignore - SecurityContext not available
    }
    return "anonymous";
  }
}
