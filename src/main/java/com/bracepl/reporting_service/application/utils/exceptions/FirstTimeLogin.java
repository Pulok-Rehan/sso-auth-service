package com.bracepl.reporting_service.application.utils.exceptions;

public class FirstTimeLogin extends SsoException {
    public FirstTimeLogin(String message) {
        super(message);
    }
}