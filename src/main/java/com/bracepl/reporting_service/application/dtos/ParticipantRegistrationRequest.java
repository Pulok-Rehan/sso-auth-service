package com.bracepl.reporting_service.application.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Request DTO for participant registration
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantRegistrationRequest {
//
//    private String firstname;
//    private String lastname;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Mobile Number is required")
    private String mobileNumber;

    private String otp;
//
//    @NotBlank(message = "Password is required")
//    @Size(min = 8, message = "Password must be at least 8 characters")
//    private String password;
}