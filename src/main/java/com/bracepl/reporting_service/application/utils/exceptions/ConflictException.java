package com.bracepl.reporting_service.application.utils.exceptions;

public class ConflictException extends SsoException {
    public ConflictException(String message) {
        super(message);
    }
}