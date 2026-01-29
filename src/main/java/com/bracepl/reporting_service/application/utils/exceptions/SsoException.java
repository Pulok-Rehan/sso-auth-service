package com.bracepl.reporting_service.application.utils.exceptions;

public class SsoException extends RuntimeException {
    public SsoException(String message) {
        super(message);
    }
    
    public SsoException(String message, Throwable cause) {
        super(message, cause);
    }
}
