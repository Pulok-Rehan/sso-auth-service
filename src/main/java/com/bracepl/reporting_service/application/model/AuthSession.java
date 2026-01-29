package com.bracepl.reporting_service.application.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Indexed;

import java.time.LocalDateTime;

/**
 * Temporary sessions for participant/moderator login flow
 * Lifecycle: Created via credential auth, deleted after 3 hours by cron
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "authsessions")
public class AuthSession {
    
    @Id
    private String mongoId;

    private String id; // 8-char NanoID
    private String projectId;
    
    private String secret; // bcrypt hashed
    
    private String role;
    
    private String userType; // "Participant" or "Moderator"
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}