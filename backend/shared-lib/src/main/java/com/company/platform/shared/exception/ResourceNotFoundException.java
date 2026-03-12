package com.company.platform.shared.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.NOT_FOUND);
    }
    
    public ResourceNotFoundException(ErrorCode errorCode, HttpStatus status) {
        super(errorCode, status);
    }
    
    @Deprecated
    public ResourceNotFoundException(String code, String message) {
        super(code, message, HttpStatus.NOT_FOUND);
    }
}
