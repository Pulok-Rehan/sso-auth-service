package com.bracepl.reporting_service.configs;

import com.bracepl.reporting_service.domain.services.AuthSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for cleanup operations
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CleanupScheduler {
    
    private final AuthSessionService authSessionService;
    
    /**
     * Delete expired auth sessions every hour
     * Sessions older than 3 hours are deleted
     */
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void deleteExpiredAuthSessions() {
        log.info("Running scheduled task: deleteExpiredAuthSessions");
        
        try {
            authSessionService.deleteExpiredSessions();
            log.info("Successfully deleted expired auth sessions");
        } catch (Exception e) {
            log.error("Error deleting expired auth sessions", e);
        }
    }
}