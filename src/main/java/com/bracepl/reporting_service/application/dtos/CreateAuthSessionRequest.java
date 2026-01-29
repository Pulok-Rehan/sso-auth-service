package com.bracepl.reporting_service.application.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * Request DTO for creating an auth session
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAuthSessionRequest {
    
    @NotBlank(message = "Project ID is required")
    @Pattern(regexp = "^[A-Z0-9]{8}$", message = "Project ID must be 8 uppercase alphanumeric characters")
    private String projectId;
    
    @NotBlank(message = "Credential secret is required")
    private String credentialSecret;
    
    @NotBlank(message = "Role is required")
    private String role;
    
    @NotBlank(message = "User type is required")
    @Pattern(regexp = "^(Participant|Moderator|Admin)$", message = "User type must be Participant or Moderator or Admin")
    private String userType;
}