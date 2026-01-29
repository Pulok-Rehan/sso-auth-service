package com.bracepl.reporting_service.application.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "projects")
public class Project {
    
    @Id
    private String mongoId;
    
    @Indexed(unique = true, sparse = true)
    private String id; // 8-char NanoID
    
    private String name;
    
    @Indexed(unique = true)
    private String slug;
    
    private String createdBy; // Admin ID
    
    private Services services;
    
    private List<Credential> credentials = new ArrayList<>();
    
    private List<AllowedRole> allowedRoles = new ArrayList<>();
    
    private Callbacks callbacks;
    
    private Boolean status = true;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    // Embedded classes
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Services {
        private String apiUrl;
        private String adminUrl;
        private String webUrl;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Credential {
        private String id; // MongoDB ObjectId as string
        private String label;
        private String secret; // bcrypt hashed
        private LocalDateTime lastUsedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllowedRole {
        private String name;
        private String slug;
        private String userType; // "Participant" or "Moderator"
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Callbacks {
        private String apiCallback;
        private String uiCallback;
        private String apiKey;
        private ApiKeyDestination apiKeyDestination;
        private String apiKeyName;
        private String uiQueryParam;
    }
}