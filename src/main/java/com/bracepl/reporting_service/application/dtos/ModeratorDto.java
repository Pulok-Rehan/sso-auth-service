package com.bracepl.reporting_service.application.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Moderator DTOs
 */
public class ModeratorDto {

    /**
     * Create Moderator Request DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        
        @NotBlank(message = "First name is required")
        private String firstname;

        @NotBlank(message = "Last name is required")
        private String lastname;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotBlank(message = "Project ID is required")
        @Size(min = 8, max = 8, message = "Project ID must be exactly 8 characters")
        @Pattern(regexp = "^[A-Z0-9]{8}$", message = "Project ID must contain only uppercase letters and numbers")
        private String projectId;

        @NotBlank(message = "Role is required")
        private String role; // lowercase role slug
    }

    /**
     * Update Moderator Request DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        
        @NotBlank(message = "First name is required")
        private String firstname;

        @NotBlank(message = "Last name is required")
        private String lastname;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address")
        private String email;
    }

    /**
     * Moderator Response DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        
        private String nanoId;
        private String publicKey;
        private String firstname;
        private String lastname;
        private String email;
        private String mobileNumber;
        private Boolean status;
        private Boolean isVerified;
        private List<ProjectDto> projects;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * Moderator List Response DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        
        private List<Response> moderators;
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
        
        private String projectId;
        
        private String createdAtStart; // YYYY-MM-DD format
        
        private String createdAtEnd; // YYYY-MM-DD format
    }

    /**
     * Assign Role Request DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignRoleRequest {
        
        @NotBlank(message = "Project ID is required")
        @Size(min = 8, max = 8, message = "Project ID must be exactly 8 characters")
        @Pattern(regexp = "^[A-Z0-9]{8}$", message = "Project ID must contain only uppercase letters and numbers")
        private String projectId;

        @NotBlank(message = "Role is required")
        private String role; // lowercase role slug
    }

    /**
     * Toggle Status Request DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToggleStatusRequest {
        
        @NotBlank(message = "Moderator ID is required")
        private String id;
    }

    /**
     * Delete Moderator Request DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeleteRequest {
        
        @NotBlank(message = "Moderator ID is required")
        private String id;
    }
}