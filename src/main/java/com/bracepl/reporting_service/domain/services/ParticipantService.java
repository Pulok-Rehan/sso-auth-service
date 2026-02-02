package com.bracepl.reporting_service.domain.services;

import com.bracepl.reporting_service.adapter.in.ParticipantController;
import com.bracepl.reporting_service.adapter.out.interfaces.ParticipantRepository;
import com.bracepl.reporting_service.adapter.out.interfaces.ProjectRepository;
import com.bracepl.reporting_service.application.dtos.ParticipantDto;
import com.bracepl.reporting_service.application.dtos.ProjectDto;
import com.bracepl.reporting_service.application.model.CallbackAction;
import com.bracepl.reporting_service.application.model.Participant;
import com.bracepl.reporting_service.application.model.Project;
import com.bracepl.reporting_service.application.utils.exceptions.ConflictException;
import com.bracepl.reporting_service.application.utils.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for admin management of participants
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantService {
    
    private final ParticipantRepository participantRepository;
    private final ProjectRepository projectRepository;
    private final CallbackService callbackService;
    
    public List<ParticipantDto.Response> getAllParticipants() {
        List<Participant> participants = participantRepository.findAll();

        // Collect all project IDs from all participants to fetch projects in bulk
        List<String> allProjectIds = participants.stream()
                .filter(participant -> participant.getProjects() != null)
                .flatMap(participant -> participant.getProjects().stream())
                .map(Participant.ProjectRole::getProjectId)
                .distinct()
                .collect(Collectors.toList());

        // Fetch projects by NanoID
        Map<String, String> projectNames = projectRepository.findByNanoIdInAndStatusTrue(allProjectIds).stream()
                .collect(Collectors.toMap(Project::getNanoId, Project::getName));

        return participants.stream()
                .map(participant -> mapToResponseWithProjects(participant, projectNames))
                .collect(Collectors.toList());
    }
    
    public Participant getParticipant(String id) {
        return participantRepository.findByNanoId(id)
                .orElseThrow(() -> new NotFoundException("Participant not found"));
    }
    
    public Page<Participant> getParticipantsPage(int page, int size, 
                                                  ParticipantController.ParticipantFilterRequest filter) {
        // Simple pagination for now
        // TODO: Implement filtering by search, projectId, createdAtStart, createdAtEnd
        return participantRepository.findAll(PageRequest.of(page, size));
    }
    
    @Transactional
    public Participant updateParticipant(String id, ParticipantController.ParticipantUpdateRequest request) {
        log.info("Updating participant: {}", id);
        
        Participant participant = getParticipant(id);
        
        // Check email uniqueness
        if (!participant.getEmail().equals(request.getEmail()) && 
            participantRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Participant with this email already exists");
        }
        
        participant.setFirstname(request.getFirstname());
        participant.setLastname(request.getLastname());
        participant.setEmail(request.getEmail().toLowerCase());
        participant.setUpdatedAt(LocalDateTime.now());
        
        Participant updated = participantRepository.save(participant);
        
        // Trigger callback
        callbackService.notifyProjectCallbacks(CallbackAction.UPDATE, updated);
        
        log.info("Participant updated: {}", id);
        return updated;
    }
    
    @Transactional
    public Participant toggleStatus(String id) {
        log.info("Toggling status for participant: {}", id);
        
        Participant participant = getParticipant(id);
        participant.setStatus(!participant.getStatus());
        participant.setUpdatedAt(LocalDateTime.now());
        
        Participant updated = participantRepository.save(participant);
        
        // Trigger callback
        callbackService.notifyProjectCallbacks(CallbackAction.UPDATE, updated);
        
        return updated;
    }
    
    @Transactional
    public void deleteParticipant(String id) {
        log.info("Deleting participant: {}", id);
        
        Participant participant = getParticipant(id);
        
        // Trigger callback before deletion
        callbackService.notifyProjectCallbacks(CallbackAction.DELETE, participant);
        
        participantRepository.delete(participant);
        
        log.info("Participant deleted: {}", id);
    }
    
    @Transactional
    public Participant assignRole(String participantId, String projectId, String role) {
        log.info("Assigning role {} to participant {} for project {}", 
                role, participantId, projectId);
        
        Participant participant = getParticipant(participantId);
        
        // Verify project exists
        projectRepository.findByIdAndStatusTrue(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found or inactive"));
        
        // Check if participant already has this project
        Participant.ProjectRole existingProjectRole = participant.getProjects().stream()
                .filter(pr -> pr.getProjectId().equals(projectId))
                .findFirst()
                .orElse(null);
        
        if (existingProjectRole != null) {
            // Add role if not already present
            if (!existingProjectRole.getRoles().contains(role.toLowerCase())) {
                existingProjectRole.getRoles().add(role.toLowerCase());
            }
        } else {
            // Add new project
            Participant.ProjectRole newProjectRole = new Participant.ProjectRole();
            newProjectRole.setProjectId(projectId);
            newProjectRole.setRoles(new ArrayList<>(List.of(role.toLowerCase())));
            participant.getProjects().add(newProjectRole);
        }
        
        participant.setUpdatedAt(LocalDateTime.now());
        Participant updated = participantRepository.save(participant);
        
        // Trigger callback for specific project
        callbackService.notifyProjectCallback(CallbackAction.UPDATE, updated, projectId);
        
        log.info("Role assigned successfully");
        return updated;
    }

    private ParticipantDto.Response mapToResponseWithProjects(Participant participant, Map<String, String> projectNames) {
        List<ProjectDto> projectDtos = new ArrayList<>();
        if (participant.getProjects() != null) {
            projectDtos = participant.getProjects().stream()
                    .map(pr -> ProjectDto.builder()
                            .projectId(pr.getProjectId())
                            .projectName(projectNames.getOrDefault(pr.getProjectId(), "Unknown Project"))
                            .roles(pr.getRoles())
                            .build())
                    .collect(Collectors.toList());
        }

        return ParticipantDto.Response.builder()
                .nanoId(participant.getNanoId())
                .publicKey(participant.getPublicKey())
                .firstname(participant.getFirstname())
                .lastname(participant.getLastname())
                .email(participant.getEmail())
                .mobileNumber(participant.getMobileNumber())
                .status(participant.getStatus())
                .isVerified(participant.getIsVerified())
                .projects(projectDtos)
                .createdAt(participant.getCreatedAt())
                .updatedAt(participant.getUpdatedAt())
                .build();
    }
}