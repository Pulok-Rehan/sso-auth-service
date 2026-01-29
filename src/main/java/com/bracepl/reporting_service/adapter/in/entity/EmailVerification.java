package com.bracepl.reporting_service.adapter.in.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Temporary storage for email verification and password reset OTPs
 * Lifecycle: Deleted after 10 minutes by cron
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "emailverifications")
public class EmailVerification {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String email; // lowercase
    
    private String otp; // 6-digit, hashed
    
    @Indexed(unique = true, sparse = true)
    private String token; // UUID v4
    
    private String userType; // "Admin", "Participant", "Moderator"
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}