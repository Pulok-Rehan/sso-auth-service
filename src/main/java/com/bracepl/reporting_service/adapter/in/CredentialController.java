
package com.bracepl.reporting_service.adapter.in;

import com.bracepl.reporting_service.adapter.in.entity.Admin;
import com.bracepl.reporting_service.application.dtos.CredentialUpdateDto;
import com.bracepl.reporting_service.application.model.ServiceResponse;
import com.bracepl.reporting_service.application.utils.exceptions.NotFoundException;
import com.bracepl.reporting_service.application.utils.exceptions.UnauthorizedException;
import com.bracepl.reporting_service.domain.services.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Credential management controller
 * All endpoints require admin authentication
 * Base path: /credential
 */
@RestController
@RequestMapping("/credential")
@RequiredArgsConstructor
@Slf4j
public class CredentialController {

    private final ProjectService projectService;

    /**
     * Create credential for project
     * POST /credential/{projectId}
     */
    @PostMapping("/{projectId}")
    public ServiceResponse createCredential(
            Authentication authentication,
            @PathVariable String projectId,
            @Valid @RequestBody CredentialCreateRequest request) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} creating credential for project: {}", admin.getNanoId(), projectId);

            CredentialResponse credential = projectService.createCredential(projectId, request, admin);
            return new ServiceResponse("Credential created successfully", credential, "201");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (UnauthorizedException e) {
            return new ServiceResponse(e.getMessage(), "401");
        } catch (Exception e) {
            log.error("Error creating credential", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @PutMapping("/reset")
    public ServiceResponse resetCredential(Authentication authentication, @RequestBody CredentialUpdateDto request) {
        log.info("Resetting credential: {} for project: {}", request.getCredentialId(), request.getProjectId());
        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} updating credential for project: {}", admin.getNanoId(), request.getProjectId());

            CredentialResponse credential = projectService.resetCredential(request.getProjectId(), request.getCredentialId(), admin);
            return new ServiceResponse("Credential updated successfully", credential, "200");
        }
        catch (Exception e){
            e.printStackTrace();
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Get paginated credentials with filters
     * POST /credential/{projectId}/listed/{offset}/{limit}
     */
    @PostMapping("/get/{projectId}")
    public ServiceResponse getCredentialsPaginated(
            Authentication authentication,
            @PathVariable String projectId,
            @RequestBody(required = false) CredentialFilterRequest filterRequest) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} fetching credentials for project: {}", admin.getNanoId(), projectId);

            // Get project with credentials (and auth check)
            var project = projectService.getProject(projectId, admin);
            var credentials = project.getCredentials();

            // Apply filtering if needed
            if (filterRequest != null && filterRequest.getSearch() != null) {
                credentials = credentials.stream()
                        .filter(c -> c.getLabel().toLowerCase().contains(filterRequest.getSearch().toLowerCase()))
                        .toList();
            }

            Map<String, Object> data = new HashMap<>();
            data.put("credentials", project.getCredentials());
            data.put("total", credentials.size());

            return new ServiceResponse("Credentials retrieved successfully", data, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (UnauthorizedException e) {
            return new ServiceResponse(e.getMessage(), "401");
        } catch (Exception e) {
            log.error("Error fetching credentials", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Delete credential
     * DELETE /credential/{projectId}/{credentialId}
     */
    @DeleteMapping("/{projectId}/{credentialId}")
    public ServiceResponse deleteCredential(
            Authentication authentication,
            @PathVariable String projectId,
            @PathVariable String credentialId) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} deleting credential {} from project: {}",
                    admin.getNanoId(), credentialId, projectId);

            projectService.deleteCredential(projectId, credentialId, admin);
            return new ServiceResponse("Credential deleted successfully", null, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (UnauthorizedException e) {
            return new ServiceResponse(e.getMessage(), "401");
        } catch (Exception e) {
            log.error("Error deleting credential", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    // DTOs

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CredentialCreateRequest {
        private String label;
//        private String secret;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CredentialFilterRequest {
        private String search;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CredentialResponse {
        private String id;
        private String label;
        private String secret; // Plain secret (only returned once)
    }
}