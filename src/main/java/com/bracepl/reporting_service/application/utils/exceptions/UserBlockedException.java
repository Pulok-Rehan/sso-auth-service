package com.bracepl.reporting_service.application.utils.exceptions;

public class UserBlockedException extends SsoException {
    public UserBlockedException(String message) {
        super(message);
    }
}