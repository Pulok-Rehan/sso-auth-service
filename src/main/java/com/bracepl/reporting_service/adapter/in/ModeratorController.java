package com.bracepl.reporting_service.adapter.in;

import com.bracepl.reporting_service.adapter.in.entity.Admin;
import com.bracepl.reporting_service.application.model.Moderator;
import com.bracepl.reporting_service.application.model.ServiceResponse;
import com.bracepl.reporting_service.application.utils.exceptions.ConflictException;
import com.bracepl.reporting_service.application.utils.exceptions.NotFoundException;
import com.bracepl.reporting_service.domain.services.ModeratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Moderator management controller (Admin operations)
 * All endpoints require admin authentication
 * Base path: /moderator
 */
@RestController
@RequestMapping("/moderator")
@RequiredArgsConstructor
@Slf4j
public class ModeratorController {

    private final ModeratorService moderatorService;

    /**
     * Create moderator
     * POST /moderator
     */
    @PostMapping
    public ServiceResponse createModerator(
            Authentication authentication,
            @Valid @RequestBody ModeratorCreateRequest request) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} creating moderator: {}", admin.getId(), request.getEmail());

            Moderator moderator = moderatorService.createModerator(request);
            return new ServiceResponse("Moderator created successfully", moderator, "201");
        } catch (ConflictException e) {
            return new ServiceResponse(e.getMessage(), "409");
        } catch (Exception e) {
            log.error("Error creating moderator", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Get all moderators
     * GET /moderator
     */
    @GetMapping
    public ServiceResponse getAllModerators(
            Authentication authentication) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} fetching all moderators", admin.getId());

            List<Moderator> moderators = moderatorService.getAllModerators();
            return new ServiceResponse("Moderators retrieved successfully", moderators, "200");
        } catch (Exception e) {
            log.error("Error fetching moderators", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Get moderator by ID
     * GET /moderator/{id}
     */
    @GetMapping("/{id}")
    public ServiceResponse getModerator(
            Authentication authentication,
            @PathVariable String id) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} fetching moderator: {}", admin.getId(), id);

            Moderator moderator = moderatorService.getModerator(id);
            return new ServiceResponse("Moderator retrieved successfully", moderator, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (Exception e) {
            log.error("Error fetching moderator", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Get paginated moderators with filters
     * POST /moderator/listed/{offset}/{limit}
     */
    @PostMapping("/listed/{offset}/{limit}")
    public ServiceResponse getModeratorsPaginated(
            Authentication authentication,
            @PathVariable int offset,
            @PathVariable int limit,
            @RequestBody(required = false) ModeratorFilterRequest filterRequest) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} fetching moderators page {}/{}", admin.getId(), offset, limit);

            Page<Moderator> moderatorsPage = moderatorService.getModeratorsPage(
                    offset / limit, limit, filterRequest);
            return new ServiceResponse("Moderators retrieved successfully", moderatorsPage, "200");
        } catch (Exception e) {
            log.error("Error fetching paginated moderators", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Update moderator
     * PUT /moderator/{id}
     */
    @PutMapping("/{id}")
    public ServiceResponse updateModerator(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody ModeratorUpdateRequest request) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} updating moderator: {}", admin.getId(), id);

            Moderator moderator = moderatorService.updateModerator(id, request);
            return new ServiceResponse("Moderator updated successfully", moderator, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (Exception e) {
            log.error("Error updating moderator", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Toggle moderator status
     * POST /moderator/toggle-status/{id}
     */
    @PostMapping("/toggle-status/{id}")
    public ServiceResponse toggleStatus(
            Authentication authentication,
            @PathVariable String id) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} toggling status for moderator: {}", admin.getId(), id);

            Moderator moderator = moderatorService.toggleStatus(id);
            return new ServiceResponse("Moderator status updated successfully", moderator, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (Exception e) {
            log.error("Error toggling moderator status", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Delete moderator
     * DELETE /moderator/{id}
     */
    @DeleteMapping("/{id}")
    public ServiceResponse deleteModerator(
            Authentication authentication,
            @PathVariable String id) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} deleting moderator: {}", admin.getId(), id);

            moderatorService.deleteModerator(id);
            return new ServiceResponse("Moderator deleted successfully", null, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (Exception e) {
            log.error("Error deleting moderator", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Assign role to moderator
     * PATCH /moderator/{id}/role
     */
    @PatchMapping("/{id}/role")
    public ServiceResponse assignRole(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody RoleAssignRequest request) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} assigning role {} to moderator: {}",
                    admin.getId(), request.getRole(), id);

            Moderator moderator = moderatorService.assignRole(
                    id, request.getProjectId(), request.getRole());
            return new ServiceResponse("Role assigned successfully", moderator, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (Exception e) {
            log.error("Error assigning role to moderator", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    // DTOs

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ModeratorCreateRequest {
        private String email;
        private String mobileNumber;
        private String projectId;
        private String role;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ModeratorUpdateRequest {
        private String firstname;
        private String email;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ModeratorFilterRequest {
        private String search;
        private String projectId;
        private String createdAtStart;
        private String createdAtEnd;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RoleAssignRequest {
        private String projectId;
        private String role;
    }
}