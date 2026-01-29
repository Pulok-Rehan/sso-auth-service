package com.bracepl.reporting_service.adapter.in.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "login_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {

    @Id
    private String id;

    @Indexed(unique = true)
    private String username; // email OR mobile

    private int failedAttempts;

    private boolean blocked;

    private boolean firstLogin; // true until first successful login

    private LocalDateTime lastAttemptAt;

    private LocalDateTime blockedAt;
}
