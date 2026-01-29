package com.bracepl.reporting_service.application.utils.exceptions;

public class NotFoundException extends SsoException {
    public NotFoundException(String message) {
        super(message);
    }
}