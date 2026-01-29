package com.bracepl.reporting_service.application.utils.exceptions;


public class OtpNotMatchException extends SsoException {
    public OtpNotMatchException(String message) {
        super(message);
    }
}
