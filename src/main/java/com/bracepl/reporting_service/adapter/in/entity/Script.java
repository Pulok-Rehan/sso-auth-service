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
 * Track execution status of migration scripts and cron jobs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "scripts")
public class Script {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String name;
    
    private LocalDateTime lastRun;
    
    private String status; // "pending", "running", "completed", "failed"
    
    private String error;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}