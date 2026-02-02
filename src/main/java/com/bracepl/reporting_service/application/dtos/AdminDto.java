package com.bracepl.reporting_service.application.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin DTOs
 */
public class AdminDto {

    /**
     * Create Admin Request DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {

        @NotBlank(message = "Mobile Number is required")
        private String mobileNumber;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address")
        private String email;

        private String projectId;
        private String role;

    }

    /**
     * Admin Response DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        
        private String nanoId;
//        private String publicKey;
        private String email;
        private String mobileNumber;
        private Boolean status;
        private Boolean isVerified;
        private List<ProjectDto> projects;
        private Boolean twoFactorEnabled;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * Admin List Response DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        
        private List<Response> admins;
        private Long total;
        private Integer currentPage;
        private Integer totalPages;
        private Integer limit;
    }

    /**
     * Filter Request DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterRequest {
        
        private String search;
        
        private String createdAtStart; // YYYY-MM-DD format
        
        private String createdAtEnd; // YYYY-MM-DD format
    }

    /**
     * Toggle Status Request DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToggleStatusRequest {
        
        @NotBlank(message = "Admin ID is required")
        private String id;
    }

    /**
     * Delete Admin Request DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteRequest {
        
        @NotBlank(message = "Admin ID is required")
        private String id;
    }

    /**
     * Analytics Response DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnalyticsResponse {
        private long totalAdmins;
        private long totalParticipants;
        private long totalModerators;
        private long totalProjects;
        private long activeProjects;
        private long totalUsers;
    }
}