package com.company.platform.shared.exception;

import com.company.platform.shared.i18n.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageService messageService;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                      HttpServletRequest request) {
        String traceId = getTraceId(request);
        List<FieldError> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldError(
                    e.getField(), 
                    messageService.getMessage(e.getDefaultMessage(), null, LocaleContextHolder.getLocale())
                ))
                .toList();
        
        String translatedMessage = messageService.getMessage(ErrorCode.VALIDATION_FAILED);
        
        log.warn("Validation failed: traceId={}, locale={}, fieldCount={}", 
            traceId, LocaleContextHolder.getLocale(), details.size());
        
        ApiError error = ApiError.builder()
                .code(ErrorCode.VALIDATION_FAILED.getCode())
                .message(translatedMessage)
                .details(details)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex,
                                                    HttpServletRequest request) {
        String traceId = getTraceId(request);
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
        
        log.warn("Business exception: code={}, traceId={}, locale={}, status={}", 
            code, traceId, LocaleContextHolder.getLocale(), ex.getStatus());
        
        ApiError error = ApiError.builder()
                .code(code)
                .message(translatedMessage)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(ex.getStatus()).body(error);
    }
    
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuthentication(AuthenticationException ex,
                                                          HttpServletRequest request) {
        String traceId = getTraceId(request);
        String translatedMessage = messageService.getMessage(ErrorCode.AUTH_TOKEN_INVALID);
        
        log.warn("Authentication failed: traceId={}, locale={}, message={}", 
            traceId, LocaleContextHolder.getLocale(), ex.getMessage());
        
        ApiError error = ApiError.builder()
                .code(ErrorCode.AUTH_TOKEN_INVALID.getCode())
                .message(translatedMessage)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                                                        HttpServletRequest request) {
        String traceId = getTraceId(request);
        String translatedMessage = messageService.getMessage(ErrorCode.AUTH_INSUFFICIENT_PERMISSION);
        
        log.warn("Access denied: traceId={}, locale={}", 
            traceId, LocaleContextHolder.getLocale());
        
        ApiError error = ApiError.builder()
                .code(ErrorCode.AUTH_INSUFFICIENT_PERMISSION.getCode())
                .message(translatedMessage)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex,
                                                   HttpServletRequest request) {
        String traceId = getTraceId(request);
        String translatedMessage = messageService.getMessage(ErrorCode.SYSTEM_INTERNAL_ERROR);
        
        log.error("Unexpected error: traceId={}, locale={}, message={}", 
            traceId, LocaleContextHolder.getLocale(), ex.getMessage(), ex);
        
        ApiError error = ApiError.builder()
                .code(ErrorCode.SYSTEM_INTERNAL_ERROR.getCode())
                .message(translatedMessage)
                .traceId(traceId)
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
    
    private String getTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Correlation-Id");
        return traceId != null ? traceId : "unknown";
    }
}
