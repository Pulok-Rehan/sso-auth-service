package com.bracepl.reporting_service.domain.services;

import com.bracepl.reporting_service.adapter.in.ModeratorController;
import com.bracepl.reporting_service.adapter.out.interfaces.ModeratorRepository;
import com.bracepl.reporting_service.adapter.out.interfaces.ProjectRepository;
import com.bracepl.reporting_service.application.dtos.ModeratorDto;
import com.bracepl.reporting_service.application.model.CallbackAction;
import com.bracepl.reporting_service.application.model.Moderator;
import com.bracepl.reporting_service.application.model.Project;
import com.bracepl.reporting_service.application.utils.NanoIdGenerator;
import com.bracepl.reporting_service.application.utils.PasswordHasher;
import com.bracepl.reporting_service.application.utils.PublicKeyGenerator;
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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class ModeratorService {
    
    private final ModeratorRepository moderatorRepository;
    private final ProjectRepository projectRepository;
    private final CallbackService callbackService;
    private final PasswordHasher passwordHasher;
//    private final PublicKeyGenerator publicKeyGenerator;
    private final NanoIdGenerator nanoIdGenerator;
    
    @Transactional
    public Moderator createModerator(ModeratorController.ModeratorCreateRequest request) {
        log.info("Creating moderator: {}", request.getEmail());
        
        // Check email uniqueness
        if (moderatorRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Moderator with this email already exists");
        }
        
        // Verify project exists
        projectRepository.findByIdAndStatusTrue(request.getProjectId())
                .orElseThrow(() -> new NotFoundException("Project not found or inactive"));
        
        Moderator moderator = new Moderator();
        moderator.setPublicKey(PublicKeyGenerator.generate());
        moderator.setId(generateUniqueModeratorId());
        moderator.setEmail(request.getEmail().toLowerCase());
        moderator.setMobileNumber(request.getMobileNumber());
        moderator.setPassword(passwordHasher.hash(request.getMobileNumber()));
        moderator.setStatus(true);
        moderator.setIsVerified(false);
        
        // Add project with role
        Moderator.ProjectRole projectRole = new Moderator.ProjectRole();
        projectRole.setProjectId(request.getProjectId());
        projectRole.setRoles(Collections.singletonList(request.getRole().toLowerCase()));
        moderator.setProjects(Collections.singletonList(projectRole));
        
        moderator.setCreatedAt(LocalDateTime.now());
        moderator.setUpdatedAt(LocalDateTime.now());
        
        Moderator saved = moderatorRepository.save(moderator);
        
        // Trigger callback
        callbackService.notifyProjectCallbacks(CallbackAction.CREATE, saved);
        
        log.info("Moderator created: {}", saved.getId());
        return saved;
    }
    
    public List<Moderator> getAllModerators() {
        return moderatorRepository.findAll();
    }
    
    public Moderator getModerator(String id) {
        return moderatorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Moderator not found"));
    }
    
    public Page<Moderator> getModeratorsPage(int page, int size, 
                                             ModeratorController.ModeratorFilterRequest filter) {
        // Simple pagination for now
        // TODO: Implement filtering by search, projectId, createdAtStart, createdAtEnd
        return moderatorRepository.findAll(PageRequest.of(page, size));
    }
    
    @Transactional
    public Moderator updateModerator(String id, ModeratorController.ModeratorUpdateRequest request) {
        log.info("Updating moderator: {}", id);
        
        Moderator moderator = getModerator(id);
        
        // Check email uniqueness
        if (!moderator.getEmail().equals(request.getEmail()) && 
            moderatorRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Moderator with this email already exists");
        }
        
        moderator.setFirstname(request.getFirstname());
//        moderator.setLastname(request.getLastname());
        moderator.setEmail(request.getEmail().toLowerCase());
        moderator.setUpdatedAt(LocalDateTime.now());
        
        Moderator updated = moderatorRepository.save(moderator);
        
        // Trigger callback
        callbackService.notifyProjectCallbacks(CallbackAction.UPDATE, updated);
        
        log.info("Moderator updated: {}", id);
        return updated;
    }
    
    @Transactional
    public Moderator toggleStatus(String id) {
        log.info("Toggling status for moderator: {}", id);
        
        Moderator moderator = getModerator(id);
        moderator.setStatus(!moderator.getStatus());
        moderator.setUpdatedAt(LocalDateTime.now());
        
        Moderator updated = moderatorRepository.save(moderator);
        
        // Trigger callback
        callbackService.notifyProjectCallbacks(CallbackAction.UPDATE, updated);
        
        return updated;
    }
    
    @Transactional
    public void deleteModerator(String id) {
        log.info("Deleting moderator: {}", id);
        
        Moderator moderator = getModerator(id);
        
        // Trigger callback before deletion
        callbackService.notifyProjectCallbacks(CallbackAction.DELETE, moderator);
        
        moderatorRepository.delete(moderator);
        
        log.info("Moderator deleted: {}", id);
    }
    
    @Transactional
    public Moderator assignRole(String moderatorId, String projectId, String role) {
        log.info("Assigning role {} to moderator {} for project {}", 
                role, moderatorId, projectId);
        
        Moderator moderator = getModerator(moderatorId);
        
        // Verify project exists
        projectRepository.findByIdAndStatusTrue(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found or inactive"));
        
        // Check if moderator already has this project
        Moderator.ProjectRole existingProjectRole = moderator.getProjects().stream()
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
            Moderator.ProjectRole newProjectRole = new Moderator.ProjectRole();
            newProjectRole.setProjectId(projectId);
            newProjectRole.setRoles(new ArrayList<>(List.of(role.toLowerCase())));
            moderator.getProjects().add(newProjectRole);
        }
        
        moderator.setUpdatedAt(LocalDateTime.now());
        Moderator updated = moderatorRepository.save(moderator);
        
        // Trigger callback for specific project
        callbackService.notifyProjectCallback(CallbackAction.UPDATE, updated, projectId);
        
        log.info("Role assigned successfully");
        return updated;
    }
    
    private String generateUniqueModeratorId() {
        String id;
        do {
            id = nanoIdGenerator.generate();
        } while (moderatorRepository.existsById(id));
        return id;
    }

    public long getTotalModerators() {
        return moderatorRepository.count();
    }

    /**
     * Find moderator by ID (internal use)
     */
    private Moderator findModeratorById(String id) {
        return moderatorRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Moderator not found with ID: " + id));
    }

    /**
     * Validate project exists and role is valid for user type
     */
    private Project validateProjectAndRole(String projectId, String roleSlug, String userType) {
        // Find project
        Project project = projectRepository.findByIdAndStatusTrue(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found or inactive with ID: " + projectId));

        // Validate role exists in project
        boolean roleExists = project.getAllowedRoles().stream()
                .anyMatch(role -> role.getSlug().equals(roleSlug) &&
                        role.getUserType().equals(userType));

        if (!roleExists) {
            throw new NotFoundException("Role '" + roleSlug + "' not found for " + userType + " in project: " + projectId);
        }

        return project;
    }

    /**
     * Generate unique NanoID
     */
    private String generateUniqueNanoId() {
        String nanoId;
        int attempts = 0;
        int maxAttempts = 10;

        do {
            nanoId = nanoIdGenerator.generate();
            attempts++;

            if (attempts > maxAttempts) {
                throw new RuntimeException("Failed to generate unique NanoID after " + maxAttempts + " attempts");
            }
        } while (moderatorRepository.existsById(nanoId));

        return nanoId;
    }

    /**
     * Parse date string to LocalDateTime
     */
//    private LocalDateTime parseDate(String dateStr, boolean startOfDay) {
//        try {
//            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
//            return startOfDay ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
//        } catch (DateTimeParseException e) {
//            throw new IllegalArgumentException("Invalid date format. Expected: yyyy-MM-dd");
//        }
//    }

    /**
     * Map Moderator entity to Response DTO
     */
    private ModeratorDto.Response mapToResponse(Moderator moderator) {
        return ModeratorDto.Response.builder()
                .id(moderator.getId())
                .publicKey(moderator.getPublicKey())
                .firstname(moderator.getFirstname())
                .lastname(moderator.getLastname())
                .email(moderator.getEmail())
                .status(moderator.getStatus())
                .isVerified(moderator.getIsVerified())
                .projects(moderator.getProjects())
                .createdAt(moderator.getCreatedAt())
                .updatedAt(moderator.getUpdatedAt())
                .build();
    }

    /**
     * Build list response with pagination info
     */
    private ModeratorDto.ListResponse buildListResponse(Page<Moderator> moderatorPage,
                                                        int offset, int limit) {
        List<ModeratorDto.Response> moderatorResponses = moderatorPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ModeratorDto.ListResponse.builder()
                .moderators(moderatorResponses)
                .total(moderatorPage.getTotalElements())
                .currentPage(offset / limit)
                .totalPages(moderatorPage.getTotalPages())
                .limit(limit)
                .build();
    }
}