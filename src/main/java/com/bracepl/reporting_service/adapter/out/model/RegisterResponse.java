package com.bracepl.reporting_service.adapter.out.model;

import lombok.Data;

@Data
public class RegisterResponse {
    private String email;
    private String mobileNumber;

    public RegisterResponse(String email, String mobileNumber){
        this.email = email;
        this.mobileNumber = mobileNumber;
    }
}
