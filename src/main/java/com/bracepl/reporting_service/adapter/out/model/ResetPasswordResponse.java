package com.bracepl.reporting_service.adapter.out.model;

import lombok.Data;

@Data
public class ResetPasswordResponse {
    private String email;
    private String mobileNumber;
}
