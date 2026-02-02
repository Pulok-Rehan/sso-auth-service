package com.bracepl.reporting_service.adapter.in.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
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

/**
 * Admin model with 2FA support
 * System administrators with full access
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "admins")
public class Admin {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String publicKey; // UUID v4 format (uppercase)
    
    @Indexed(unique = true, sparse = true)
    private String nanoId; // 8-char NanoID
    
    private String firstname;
    
    private String lastname;
    
    @Indexed(unique = true)
    private String email;

    @Indexed(unique = true)
    private String mobileNumber;
    
    private String password; // bcrypt hashed
    
    private Boolean status = true;
    
    private Boolean isVerified = false;
    
    // Two-Factor Authentication fields
    private Boolean twoFactorEnabled = false;
    private Boolean isSuperAdmin = false;
    
    private String twoFactorSecretEnc; // AES-256-GCM encrypted
    
    private Long twoFactorLastTimeStep; // For replay protection
    
    private String twoFactorTempSecretEnc; // Temporary secret during setup
    
    private List<String> twoFactorBackupCodes = new ArrayList<>(); // bcrypt hashed

    private List<ProjectRole> projects = new ArrayList<>();
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectRole {
        @Indexed
        private String projectId;
        private List<String> roles; // Array of role slugs
    }
}