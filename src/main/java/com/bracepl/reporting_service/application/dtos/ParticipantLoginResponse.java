package com.bracepl.reporting_service.application.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for participant login
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipantLoginResponse {
    
    private String token;
    private ParticipantProfile profile;
    private String userType;
    private String uiCallback;
    
    /**
     * Participant profile DTO
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantProfile {
        
        private String id;
        private String publicKey;
        private String firstname;
        private String lastname;
        private String email;
        private Boolean status;
        private Boolean isVerified;
        private List<ProjectRoleInfo> projects;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class ProjectRoleInfo {
            private String projectId;
            private List<String> roles;
        }
    }
}