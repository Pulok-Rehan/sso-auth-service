package com.bracepl.reporting_service.domain.services;

import com.bracepl.reporting_service.adapter.in.entity.Admin;
import com.bracepl.reporting_service.adapter.out.interfaces.CallbackLogRepository;
import com.bracepl.reporting_service.adapter.out.interfaces.ProjectRepository;
import com.bracepl.reporting_service.application.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for handling webhook callbacks to project APIs
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CallbackService {
    
    private final ProjectRepository projectRepository;
    private final CallbackLogRepository callbackLogRepository;
    private final WebClient.Builder webClientBuilder;
    
    @Value("${app.callback.timeout-seconds}")
    private int timeoutSeconds;
    
    /**
     * Notify all projects associated with a user about an action
     */
    public void notifyProjectCallbacks(CallbackAction action, Object user) {
        List<String> projectIds = extractProjectIds(user);
        
        if (projectIds.isEmpty()) {
            log.debug("No projects to notify for user action: {}", action);
            return;
        }
        
        // Get all active projects
        List<Project> projects = projectRepository.findByNanoIdInAndStatusTrue(projectIds);
        
        if (projects.isEmpty()) {
            log.debug("No active projects found for callback");
            return;
        }
        
        log.info("Notifying {} projects about {} action", projects.size(), action.getValue());
        
        // Execute callbacks in parallel
        projects.parallelStream().forEach(project -> {
            if (project.getCallbacks() != null) {
                // 1. Notify API Callback
                if (project.getCallbacks().getApiCallback() != null) {
                    executeCallback(action, user, project, false);
                }
                
                // 2. Notify Admin Callback
                if (project.getCallbacks().getAdminCallback() != null) {
                    executeCallback(action, user, project, true);
                }
            }
        });
    }
    
    /**
     * Notify a specific project about a user action
     */
    public void notifyProjectCallback(CallbackAction action, Object user, String projectId) {
        Project project = projectRepository.findByIdAndStatusTrue(projectId)
                .orElse(null);
        
        if (project == null) {
            log.warn("Project not found or inactive: {}", projectId);
            return;
        }
        
        if (project.getCallbacks() != null) {
            // 1. Notify API Callback
            if (project.getCallbacks().getApiCallback() != null) {
                executeCallback(action, user, project, false);
            }
            
            // 2. Notify Admin Callback
            if (project.getCallbacks().getAdminCallback() != null) {
                executeCallback(action, user, project, true);
            }
        }
    }
    
    /**
     * Execute callback to project API
     */
    private void executeCallback(CallbackAction action, Object user, Project project, boolean isAdminCallback) {
        long startTime = System.currentTimeMillis();
        
        // Build payload
        Map<String, Object> payload = buildPayload(action, user, project.getNanoId());
        
        // Build headers
        Map<String, String> headers = buildHeaders(project);
        
        // Add API key to body if needed
        if (project.getCallbacks().getApiKeyDestination() != null &&
            "body".equalsIgnoreCase(project.getCallbacks().getApiKeyDestination().getValue())) {
            payload.put(project.getCallbacks().getApiKeyName(), 
                       project.getCallbacks().getApiKey());
        }
        
        String callbackUrl;
        if (isAdminCallback) {
            callbackUrl = project.getCallbacks().getAdminCallback();
            // Append admin query param if exists
            if (project.getCallbacks().getAdminQueryParam() != null) {
                String separator = callbackUrl.contains("?") ? "&" : "?";
                callbackUrl += separator + project.getCallbacks().getAdminQueryParam();
            }
        } else {
            callbackUrl = project.getCallbacks().getApiCallback();
        }

        String userId = extractUserId(user);
        String userEmail = extractUserEmail(user);
        
        log.info("Executing callback to {} for action: {}", callbackUrl, action.getValue());
        
        try {
            // Build WebClient
            WebClient webClient = webClientBuilder.build();
            
            // Make HTTP POST request
            String response = webClient.post()
                    .uri(callbackUrl)
                    .headers(httpHeaders -> headers.forEach(httpHeaders::add))
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .onErrorResume(e -> Mono.just("Error: " + e.getMessage()))
                    .block();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            // Log successful callback
            logCallback(project.getNanoId(), action, callbackUrl, userId, userEmail,
                       payload, headers, 200, response, null, true, responseTime);
            
            log.info("Callback successful for project: {} ({}ms)", project.getNanoId(), responseTime);
            
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            
            log.error("Callback failed for project: {}", project.getNanoId(), e);
            
            // Log failed callback
            logCallback(project.getNanoId(), action, callbackUrl, userId, userEmail,
                       payload, headers, 0, null, e.getMessage(), false, responseTime);
        }
    }
    
    /**
     * Build callback payload
     */
    private Map<String, Object> buildPayload(CallbackAction action, Object user, String projectId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("action", action.getValue());
        
        Map<String, Object> userMap = new HashMap<>();
        
        if (user instanceof Participant) {
            Participant participant = (Participant) user;
            userMap.put("id", participant.getNanoId());
            userMap.put("email", participant.getEmail());
            userMap.put("phone", participant.getMobileNumber());
            userMap.put("public_key", participant.getPublicKey());
            userMap.put("status", participant.getStatus());
            userMap.put("isVerified", participant.getIsVerified());
            
            // Get roles for this specific project
            List<String> roles = participant.getProjects().stream()
                    .filter(p -> p.getProjectId().equals(projectId))
                    .findFirst()
                    .map(Participant.ProjectRole::getRoles)
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(role -> role != null && !role.trim().isEmpty() && !role.equalsIgnoreCase("null"))
                    .collect(Collectors.toList());
            userMap.put("roles", roles);
            
            payload.put("userType", "Participant");
        } else if (user instanceof Moderator) {
            Moderator moderator = (Moderator) user;
            userMap.put("id", moderator.getNanoId());
            userMap.put("email", moderator.getEmail());
            userMap.put("phone", moderator.getMobileNumber());
            userMap.put("public_key", moderator.getPublicKey());
            userMap.put("status", moderator.getStatus());
            userMap.put("isVerified", moderator.getIsVerified());
            
            // Get roles for this specific project
            List<String> roles = moderator.getProjects().stream()
                    .filter(p -> p.getProjectId().equals(projectId))
                    .findFirst()
                    .map(Moderator.ProjectRole::getRoles)
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(role -> role != null && !role.trim().isEmpty() && !role.equalsIgnoreCase("null"))
                    .collect(Collectors.toList());
            userMap.put("roles", roles);
            
            payload.put("userType", "Moderator");
        } else if (user instanceof Admin) {
            Admin admin = (Admin) user;
            userMap.put("id", admin.getNanoId());
            userMap.put("email", admin.getEmail());
            userMap.put("phone", admin.getMobileNumber());
            userMap.put("public_key", admin.getPublicKey());
            userMap.put("status", admin.getStatus());
            userMap.put("isVerified", admin.getIsVerified());
            
            // Get roles for this specific project
            List<String> roles = Collections.emptyList();
            if (admin.getProjects() != null) {
                roles = admin.getProjects().stream()
                        .filter(p -> p.getProjectId().equals(projectId))
                        .findFirst()
                        .map(Admin.ProjectRole::getRoles)
                        .orElse(Collections.emptyList())
                        .stream()
                        .filter(role -> role != null && !role.trim().isEmpty() && !role.equalsIgnoreCase("null"))
                        .collect(Collectors.toList());
            }
            userMap.put("roles", roles);
            
            payload.put("userType", "Admin");
        }
        
        payload.put("user", userMap);
        return payload;
    }
    
    /**
     * Build callback headers
     */
    private Map<String, String> buildHeaders(Project project) {
        Map<String, String> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        
        // Add API key to header if configured
        if (project.getCallbacks().getApiKeyDestination() != null &&
            "header".equalsIgnoreCase(project.getCallbacks().getApiKeyDestination().getValue())) {
            headers.put(project.getCallbacks().getApiKeyName(), 
                       project.getCallbacks().getApiKey());
        }
        
        return headers;
    }
    
    /**
     * Log callback execution
     */
    private void logCallback(String projectId, CallbackAction action, String callbackUrl,
                            String userId, String userEmail, Map<String, Object> requestPayload,
                            Map<String, String> requestHeaders, int responseStatus,
                            String responseMessage, String errorDetails, boolean success,
                            long responseTime) {
        
        CallbackLog log = new CallbackLog();
        log.setProjectId(projectId);
        log.setAction(action.getValue());
        log.setCallbackUrl(callbackUrl);
        log.setUserId(userId);
        log.setUserEmail(userEmail);
        log.setRequestPayload(requestPayload);
        
        // Redact API key from headers in log
        Map<String, String> redactedHeaders = new HashMap<>(requestHeaders);
        redactedHeaders.replaceAll((k, v) -> "***REDACTED***");
        log.setRequestHeaders(redactedHeaders);
        
        log.setResponseStatus(responseStatus);
        log.setResponseMessage(responseMessage);
        log.setErrorDetails(errorDetails);
        log.setSuccess(success);
        log.setRetryCount(0);
        log.setResponseTime(responseTime);
        log.setCreatedAt(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());
        
        callbackLogRepository.save(log);
    }
    
    /**
     * Extract project IDs from user
     */
    private List<String> extractProjectIds(Object user) {
        if (user instanceof Participant) {
            return ((Participant) user).getProjects().stream()
                    .map(Participant.ProjectRole::getProjectId)
                    .collect(Collectors.toList());
        } else if (user instanceof Moderator) {
            return ((Moderator) user).getProjects().stream()
                    .map(Moderator.ProjectRole::getProjectId)
                    .collect(Collectors.toList());
        } else if (user instanceof Admin) {
            if (((Admin) user).getProjects() == null) return Collections.emptyList();
            return ((Admin) user).getProjects().stream()
                    .map(Admin.ProjectRole::getProjectId)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
    
    /**
     * Extract user ID
     */
    private String extractUserId(Object user) {
        if (user instanceof Participant) {
            return ((Participant) user).getNanoId();
        } else if (user instanceof Moderator) {
            return ((Moderator) user).getNanoId();
        } else if (user instanceof Admin) {
            return ((Admin) user).getNanoId();
        }
        return null;
    }
    
    /**
     * Extract user email
     */
    private String extractUserEmail(Object user) {
        if (user instanceof Participant) {
            return ((Participant) user).getEmail();
        } else if (user instanceof Moderator) {
            return ((Moderator) user).getEmail();
        } else if (user instanceof Admin) {
            return ((Admin) user).getEmail();
        }
        return null;
    }
}