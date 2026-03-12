package com.company.platform.shared.exception;

import org.springframework.http.HttpStatus;

public class DuplicateResourceException extends BusinessException {

    @Deprecated
    public DuplicateResourceException(String code, String message) {
        super(code, message, HttpStatus.CONFLICT);
    }
    
    public DuplicateResourceException(ErrorCode errorCode, HttpStatus status) {
        super(errorCode, status);
    }
    
    public DuplicateResourceException(ErrorCode errorCode) {
        super(errorCode, HttpStatus.CONFLICT);
    }
}
