package com.bracepl.reporting_service.application.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Participant DTOs
 */
public class ParticipantDto {

    /**
     * Participant Response DTO
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
     * Participant List Response DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ListResponse {
        
        private List<Response> participants;
        private Long total;
        private Integer currentPage;
        private Integer totalPages;
        private Integer limit;
    }
}