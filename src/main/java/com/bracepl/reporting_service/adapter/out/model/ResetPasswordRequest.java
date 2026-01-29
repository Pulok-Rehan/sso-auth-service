package com.bracepl.reporting_service.adapter.out.model;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String username;
    private String oldPassword;
    private String newPassword;
    private String userType;
}
