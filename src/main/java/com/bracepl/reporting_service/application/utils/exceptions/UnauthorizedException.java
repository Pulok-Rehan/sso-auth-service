package com.bracepl.reporting_service.application.utils.exceptions;

public class UnauthorizedException extends SsoException {
    public UnauthorizedException(String message) {
        super(message);
    }
}