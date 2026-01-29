package com.bracepl.reporting_service.application.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for auth session creation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAuthSessionResponse {
    private String id;
    private String projectId;
    private String role;
    private String userType;
    private String secret;
//    private AuthSessionData authSession;
//
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    public static class AuthSessionData {
// // Plain secret (only returned once)
//    }
}