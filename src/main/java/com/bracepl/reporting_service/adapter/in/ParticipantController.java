package com.bracepl.reporting_service.adapter.in;

import com.bracepl.reporting_service.adapter.in.entity.Admin;
import com.bracepl.reporting_service.application.dtos.ParticipantDto;
import com.bracepl.reporting_service.application.model.Participant;
import com.bracepl.reporting_service.application.model.ServiceResponse;
import com.bracepl.reporting_service.application.utils.exceptions.NotFoundException;
import com.bracepl.reporting_service.domain.services.ParticipantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Participant management controller (Admin operations)
 * All endpoints require admin authentication
 * Base path: /participant
 */
@RestController
@RequestMapping("/participant")
@RequiredArgsConstructor
@Slf4j
public class ParticipantController {

    private final ParticipantService participantService;

    /**
     * Get all participants
     * GET /participant
     */
    @GetMapping
    public ServiceResponse getAllParticipants(
            Authentication authentication) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} fetching all participants", admin.getNanoId());

            List<ParticipantDto.Response> participants = participantService.getAllParticipants();
            return new ServiceResponse("Participants retrieved successfully", participants, "200");
        } catch (Exception e) {
            log.error("Error fetching participants", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Get participant by ID
     * GET /participant/{id}
     */
    @GetMapping("/{id}")
    public ServiceResponse getParticipant(
            Authentication authentication,
            @PathVariable String id) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} fetching participant: {}", admin.getNanoId(), id);

            Participant participant = participantService.getParticipant(id);
            return new ServiceResponse("Participant retrieved successfully", participant, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (Exception e) {
            log.error("Error fetching participant", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Get paginated participants with filters
     * POST /participant/listed/{offset}/{limit}
     */
    @PostMapping("/listed/{offset}/{limit}")
    public ServiceResponse getParticipantsPaginated(
            Authentication authentication,
            @PathVariable int offset,
            @PathVariable int limit,
            @RequestBody(required = false) ParticipantFilterRequest filterRequest) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} fetching participants page {}/{}", admin.getNanoId(), offset, limit);

            Page<Participant> participantsPage = participantService.getParticipantsPage(
                    offset / limit, limit, filterRequest);
            return new ServiceResponse("Participants retrieved successfully", participantsPage, "200");
        } catch (Exception e) {
            log.error("Error fetching paginated participants", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Update participant
     * PUT /participant/{id}
     */
    @PutMapping("/{id}")
    public ServiceResponse updateParticipant(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody ParticipantUpdateRequest request) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} updating participant: {}", admin.getNanoId(), id);

            Participant participant = participantService.updateParticipant(id, request);
            return new ServiceResponse("Participant updated successfully", participant, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (Exception e) {
            log.error("Error updating participant", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Toggle participant status
     * POST /participant/toggle-status/{id}
     */
    @PostMapping("/toggle-status/{id}")
    public ServiceResponse toggleStatus(
            Authentication authentication,
            @PathVariable String id) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} toggling status for participant: {}", admin.getNanoId(), id);

            Participant participant = participantService.toggleStatus(id);
            return new ServiceResponse("Participant status updated successfully", participant, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (Exception e) {
            log.error("Error toggling participant status", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Delete participant
     * DELETE /participant/{id}
     */
    @DeleteMapping("/{id}")
    public ServiceResponse deleteParticipant(
            Authentication authentication,
            @PathVariable String id) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} deleting participant: {}", admin.getNanoId(), id);

            participantService.deleteParticipant(id);
            return new ServiceResponse("Participant deleted successfully", null, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (Exception e) {
            log.error("Error deleting participant", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Assign role to participant
     * PATCH /participant/{id}/role
     */
    @PatchMapping("/{id}/role")
    public ServiceResponse assignRole(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody RoleAssignRequest request) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} assigning role {} to participant: {}",
                    admin.getNanoId(), request.getRole(), id);

            Participant participant = participantService.assignRole(
                    id, request.getProjectId(), request.getRole());
            return new ServiceResponse("Role assigned successfully", participant, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (Exception e) {
            log.error("Error assigning role to participant", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    // DTOs

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ParticipantUpdateRequest {
        private String firstname;
        private String lastname;
        private String email;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ParticipantFilterRequest {
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