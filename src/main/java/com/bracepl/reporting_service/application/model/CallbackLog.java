package com.bracepl.reporting_service.application.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Audit trail for webhook callbacks to project APIs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "callbacklogs")
@CompoundIndexes({
    @CompoundIndex(name = "success_createdAt_idx", def = "{'success': 1, 'createdAt': 1}"),
    @CompoundIndex(name = "projectId_success_idx", def = "{'projectId': 1, 'success': 1}"),
    @CompoundIndex(name = "userId_createdAt_idx", def = "{'userId': 1, 'createdAt': 1}")
})
public class CallbackLog {
    
    @Id
    private String id;
    
    @Indexed
    private String projectId;
    
    private String action; // "create", "update", "delete"
    
    private String callbackUrl;
    
    @Indexed
    private String userId;
    
    private String userEmail;
    
    private Map<String, Object> requestPayload;
    
    private Map<String, String> requestHeaders;
    
    private Integer responseStatus;
    
    private String responseMessage;
    
    private String errorDetails;
    
    private Boolean success = false;
    
    private Integer retryCount = 0;
    
    private Long responseTime; // milliseconds
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
}