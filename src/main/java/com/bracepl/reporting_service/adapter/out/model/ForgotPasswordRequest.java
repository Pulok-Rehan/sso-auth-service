package com.bracepl.reporting_service.adapter.out.model;

import lombok.Data;

@Data
public class ForgotPasswordRequest {
    private String email;
    private String mobileNumber;
    private String userType;
    private String otp;
}
