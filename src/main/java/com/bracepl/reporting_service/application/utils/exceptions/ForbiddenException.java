package com.bracepl.reporting_service.application.utils.exceptions;

public class ForbiddenException extends SsoException {
    public ForbiddenException(String message) {
        super(message);
    }
}