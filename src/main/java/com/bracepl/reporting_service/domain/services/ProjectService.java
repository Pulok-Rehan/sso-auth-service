package com.bracepl.reporting_service.domain.services;

import com.bracepl.reporting_service.adapter.in.ProjectController;
import com.bracepl.reporting_service.adapter.in.RoleController;
import com.bracepl.reporting_service.adapter.in.CredentialController;
import com.bracepl.reporting_service.adapter.in.entity.Admin;
import com.bracepl.reporting_service.adapter.out.interfaces.AdminRepository;
import com.bracepl.reporting_service.adapter.out.interfaces.ProjectRepository;
import com.bracepl.reporting_service.application.dtos.CreateProjectDto;
import com.bracepl.reporting_service.application.model.Project;
import com.bracepl.reporting_service.application.utils.CredentialSecretGenerator;
import com.bracepl.reporting_service.application.utils.NanoIdGenerator;
import com.bracepl.reporting_service.application.utils.PasswordHasher;
import com.bracepl.reporting_service.application.utils.exceptions.ConflictException;
import com.bracepl.reporting_service.application.utils.exceptions.NotFoundException;
import com.bracepl.reporting_service.application.utils.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for project management
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectService {
    
    private final ProjectRepository projectRepository;
    private final NanoIdGenerator nanoIdGenerator;
    private final PasswordHasher passwordHasher;
    private final AdminRepository adminRepository;
    
    @Transactional
    public CreateProjectDto createProject(ProjectController.ProjectCreateRequest request, Admin admin) {
        log.info("Creating project: {}", request.getName());
        
        if (projectRepository.existsBySlug(request.getName().replace(" ", "-"))) {
            throw new ConflictException("Project with this slug already exists");
        }
        
        Project project = new Project();
        project.setNanoId(generateUniqueProjectId());
        project.setName(request.getName());
        project.setSlug(request.getName().toLowerCase().replace(" ", "-"));
        project.setCreatedBy(admin.getNanoId());
        project.setServices(request.getServices());
        project.setCallbacks(request.getCallbacks());
        project.setStatus(true);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        
        Project saved = projectRepository.save(project);
        String generatedSecret = CredentialSecretGenerator.generate();
        this.createCredential(saved, saved.getSlug() + UUID.randomUUID(), generatedSecret);
        log.info("Project created: {}", saved.getNanoId());
        Optional<Project> savedProject = projectRepository.findByNanoId(saved.getNanoId());
        if (savedProject.isEmpty()){
            throw new NotFoundException("Project not found");
        }
        return CreateProjectDto.builder()
                .project(savedProject.get())
                .credential(generatedSecret)
                .build();
    }
    
    public Project getProject(String id, Admin admin) {
        Project project = projectRepository.findByNanoIdAndStatusTrue(id)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        
//        if (!isSuperAdmin(admin) && !project.getCreatedBy().equals(admin.getId())) {
//            throw new UnauthorizedException("You are not authorized to view this project");
//        }
        if (!project.getCreatedBy().equals(admin.getNanoId())) {
            if (isSuperAdmin(admin)){
                return project;
            }
            throw new UnauthorizedException("You are not authorized to view this project");
        }
        return project;
    }

    public List<Project> getAllProjects(Admin admin) {
        if (isSuperAdmin(admin)) {
            return projectRepository.findAll();
        }
        return projectRepository.findByCreatedBy(admin.getNanoId());
    }
    
    public Page<Project> getProjectsPage(int page, int size, Admin admin) {
        if (isSuperAdmin(admin)) {
            return projectRepository.findAll(PageRequest.of(page, size));
        }
        return projectRepository.findByCreatedBy(admin.getNanoId(), PageRequest.of(page, size));
    }
    
    @Transactional
    public Project updateProject(String id, ProjectController.ProjectUpdateRequest request, Admin admin) {
        log.info("Updating project: {}", id);
        
        Project project = getProject(id, admin); // Authorization check is inside getProject
        
        if (!project.getSlug().equals(request.getSlug()) && 
            projectRepository.existsBySlug(request.getSlug())) {
            throw new ConflictException("Project with this slug already exists");
        }
        
        project.setName(request.getName());
        project.setSlug(request.getName().toLowerCase().replace(" ", "-"));
        project.setServices(request.getServices());
        project.setCallbacks(request.getCallbacks());
        project.setUpdatedAt(LocalDateTime.now());
        
        return projectRepository.save(project);
    }
    
    @Transactional
    public void deleteProject(String id, Admin admin) {
        log.info("Deleting project: {}", id);
        Project project = getProject(id, admin); // Authorization check
        projectRepository.delete(project);
    }
    
    @Transactional
    public Project toggleStatus(String id, Admin admin) {
        Project project = projectRepository.findByNanoId(id)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        
        if (!isSuperAdmin(admin) && !project.getCreatedBy().equals(admin.getNanoId())) {
            throw new UnauthorizedException("You are not authorized to change the status of this project");
        }
        
        project.setStatus(!project.getStatus());
        project.setUpdatedAt(LocalDateTime.now());
        return projectRepository.save(project);
    }
    
    // Credential Management
    
    @Transactional
    public CredentialController.CredentialResponse createCredential(String projectId, CredentialController.CredentialCreateRequest credentialCreateRequest, Admin admin) {
        log.info("Creating credential for project: {}", projectId);

        Project project = getProject(projectId, admin); // Authorization check
        String generatedCredential = nanoIdGenerator.generate();
        String hashedSecret = passwordHasher.hash(generatedCredential);

        Project.Credential credential = new Project.Credential();
        credential.setId(UUID.randomUUID().toString());
        credential.setLabel(credentialCreateRequest.getLabel());
        credential.setSecret(hashedSecret);
        credential.setCreatedAt(LocalDateTime.now());
        credential.setUpdatedAt(LocalDateTime.now());

        project.getCredentials().add(credential);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);

        log.info("Credential created for project: {}", projectId);

        return new CredentialController.CredentialResponse(credential.getId(), credentialCreateRequest.getLabel(), generatedCredential);
    }

    @Transactional
    public CredentialController.CredentialResponse resetCredential(String projectId, String credentialId, Admin admin) {
        Project project = getProject(projectId, admin); // Authorization check
        Project.Credential credential = project.getCredentials().stream()
                .filter(c -> c.getId().equals(credentialId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Credential not found"));
        
        String newSecret = CredentialSecretGenerator.generate();
        String hashedSecret = passwordHasher.hash(newSecret);
        
        credential.setSecret(hashedSecret);
        credential.setUpdatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
        
        log.info("Credential reset successfully for project: {}", projectId);
        return new CredentialController.CredentialResponse(credential.getId(), credential.getLabel(), newSecret);
    }

    @Transactional
    public void deleteCredential(String projectId, String credentialId, Admin admin) {
        log.info("Deleting credential: {} from project: {}", credentialId, projectId);
        
        Project project = getProject(projectId, admin); // Authorization check
        project.getCredentials().removeIf(c -> c.getId().equals(credentialId));
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
    }
    
    // Role Management
    
    @Transactional
    public Project.AllowedRole createRole(String projectId, RoleController.RoleCreateRequest request, Admin admin) {
        log.info("Creating role for project: {}", projectId);
        
        Project project = getProject(projectId, admin); // Authorization check
        
        boolean exists = project.getAllowedRoles().stream()
                .anyMatch(r -> r.getSlug().equals(request.getName().toLowerCase().replace(" ", "-")));
        
        if (exists) {
            throw new ConflictException("Role with this slug already exists");
        }
        
        Project.AllowedRole role = new Project.AllowedRole();
        role.setName(request.getName());
        role.setSlug(request.getName().toLowerCase().replace(" ", "-"));
        role.setUserType(request.getUserType());
        
        project.getAllowedRoles().add(role);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
        
        log.info("Role created: {}", request.getName().toLowerCase().replace(" ", "-"));
        return role;
    }
    
    @Transactional
    public Project.AllowedRole updateRole(String projectId, String roleSlug, String newName, Admin admin) {
        log.info("Updating role: {} in project: {}", roleSlug, projectId);
        
        Project project = getProject(projectId, admin); // Authorization check
        
        Project.AllowedRole role = project.getAllowedRoles().stream()
                .filter(r -> r.getSlug().equals(roleSlug))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Role not found"));
        
        role.setName(newName);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
        
        return role;
    }
    
    @Transactional
    public void deleteRole(String projectId, String roleSlug, Admin admin) {
        log.info("Deleting role: {} from project: {}", roleSlug, projectId);
        
        Project project = getProject(projectId, admin); // Authorization check
        project.getAllowedRoles().removeIf(r -> r.getSlug().equals(roleSlug));
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
    }

    @Transactional
    public Project.AllowedRole toggleRoleStatus(String projectId, String roleSlug, Admin admin) {
        log.info("Toggling status for role: {} in project: {}", roleSlug, projectId);
        
        Project project = getProject(projectId, admin); // Authorization check
        
        Project.AllowedRole role = project.getAllowedRoles().stream()
                .filter(r -> r.getSlug().equals(roleSlug))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Role not found"));
        
        role.setStatus(!role.getStatus());
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);
        
        return role;
    }
    
    private String generateUniqueProjectId() {
        String id;
        do {
            id = nanoIdGenerator.generate();
        } while (projectRepository.existsById(id));
        return id;
    }

    private boolean isSuperAdmin(Admin admin) {
        return Boolean.TRUE.equals(admin.getIsSuperAdmin());
    }

    // This method is for internal use when creating a project
    private Project.Credential createCredential(Project project, String label, String secret) {
        log.info("Creating credential for project: {}", project.getNanoId());

        String hashedSecret = passwordHasher.hash(secret);

        Project.Credential credential = new Project.Credential();
        credential.setId(UUID.randomUUID().toString());
        credential.setLabel(label);
        credential.setSecret(hashedSecret);
        credential.setCreatedAt(LocalDateTime.now());
        credential.setUpdatedAt(LocalDateTime.now());

        project.getCredentials().add(credential);
        project.setUpdatedAt(LocalDateTime.now());
        projectRepository.save(project);

        log.info("Credential created for project: {}", project.getNanoId());

        return credential;
    }
}