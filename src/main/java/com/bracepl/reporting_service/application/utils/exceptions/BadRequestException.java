package com.bracepl.reporting_service.application.utils.exceptions;

public class BadRequestException extends SsoException {
    public BadRequestException(String message) {
        super(message);
    }
}
