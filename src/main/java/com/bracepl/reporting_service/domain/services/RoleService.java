package com.bracepl.reporting_service.domain.services;

import com.bracepl.reporting_service.adapter.in.entity.Admin;
import com.bracepl.reporting_service.adapter.out.interfaces.AdminRepository;
import com.bracepl.reporting_service.adapter.out.interfaces.ModeratorRepository;
import com.bracepl.reporting_service.adapter.out.interfaces.ParticipantRepository;
import com.bracepl.reporting_service.application.model.Moderator;
import com.bracepl.reporting_service.application.model.Participant;
import com.bracepl.reporting_service.application.model.UserType;
import com.bracepl.reporting_service.application.utils.exceptions.NotFoundException;
import com.bracepl.reporting_service.application.utils.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final AdminRepository adminRepository;
    private final ParticipantRepository participantRepository;
    private final ModeratorRepository moderatorRepository;
    private final ProjectService projectService;

    @Transactional
    public void assignRole(Admin assigningAdmin, String userId, String userTypeStr, String projectId, String roleSlug) {
        UserType userType = UserType.fromValue(userTypeStr);
        checkAuthorization(assigningAdmin, userType, projectId);

        // Perform Assignment
        switch (userType) {
            case ADMIN:
                assignRoleToAdmin(userId, projectId, roleSlug);
                break;
            case PARTICIPANT:
                assignRoleToParticipant(userId, projectId, roleSlug);
                break;
            case MODERATOR:
                assignRoleToModerator(userId, projectId, roleSlug);
                break;
            default:
                throw new IllegalArgumentException("Unsupported user type for role assignment");
        }
    }

    @Transactional
    public void unassignRole(Admin assigningAdmin, String userId, String userTypeStr, String projectId, String roleSlug) {
        UserType userType = UserType.fromValue(userTypeStr);
        checkAuthorization(assigningAdmin, userType, projectId);

        // Perform Unassignment
        switch (userType) {
            case ADMIN:
                unassignRoleFromAdmin(userId, projectId, roleSlug);
                break;
            case PARTICIPANT:
                unassignRoleFromParticipant(userId, projectId, roleSlug);
                break;
            case MODERATOR:
                unassignRoleFromModerator(userId, projectId, roleSlug);
                break;
            default:
                throw new IllegalArgumentException("Unsupported user type for role unassignment");
        }
    }

    @Transactional
    public void deleteRole(String projectId, String roleSlug, Admin admin) {
        // 1. Delete role from project definition
        projectService.deleteRole(projectId, roleSlug, admin);

        // 2. Remove role from all users
        adminRepository.removeRoleFromAllUsers(projectId, roleSlug);
        participantRepository.removeRoleFromAllUsers(projectId, roleSlug);
        moderatorRepository.removeRoleFromAllUsers(projectId, roleSlug);
        
        log.info("Role {} deleted from project {} and removed from all users", roleSlug, projectId);
    }

    private void checkAuthorization(Admin assigningAdmin, UserType targetUserType, String projectId) {
        if (targetUserType == UserType.ADMIN) {
            if (!Boolean.TRUE.equals(assigningAdmin.getIsSuperAdmin())) {
                throw new UnauthorizedException("Only SuperAdmins can manage roles for other Admins");
            }
        } else {
            // For Participants and Moderators, check if admin has access to the project
            boolean hasAccess = Boolean.TRUE.equals(assigningAdmin.getIsSuperAdmin()) ||
                    (assigningAdmin.getProjects() != null && assigningAdmin.getProjects().stream()
                            .anyMatch(p -> p.getProjectId().equals(projectId)));

            if (!hasAccess) {
                throw new UnauthorizedException("You do not have access to manage roles for this project");
            }
        }
    }

    public UserType getUserType(String userId) {
        if (adminRepository.existsById(userId)) {
            return UserType.ADMIN;
        } else if (moderatorRepository.existsById(userId)) {
            return UserType.MODERATOR;
        } else if (participantRepository.existsById(userId)) {
            return UserType.PARTICIPANT;
        } else {
            throw new NotFoundException("User not found with ID: " + userId);
        }
    }

    private void assignRoleToAdmin(String adminId, String projectId, String roleSlug) {
        Admin admin = adminRepository.findByNanoId(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found"));

        if (admin.getProjects() == null) {
            admin.setProjects(new ArrayList<>());
        }
        updateAdminProjectRole(admin.getProjects(), projectId, roleSlug);
        adminRepository.save(admin);
    }

    private void assignRoleToParticipant(String participantId, String projectId, String roleSlug) {
        Participant participant = participantRepository.findByNanoId(participantId)
                .orElseThrow(() -> new NotFoundException("Participant not found"));

        if (participant.getProjects() == null) {
            participant.setProjects(new ArrayList<>());
        }
        updateParticipantProjectRole(participant.getProjects(), projectId, roleSlug);
        participantRepository.save(participant);
    }

    private void assignRoleToModerator(String moderatorId, String projectId, String roleSlug) {
        Moderator moderator = moderatorRepository.findByNanoId(moderatorId)
                .orElseThrow(() -> new NotFoundException("Moderator not found"));

        if (moderator.getProjects() == null) {
            moderator.setProjects(new ArrayList<>());
        }
        updateModeratorProjectRole(moderator.getProjects(), projectId, roleSlug);
        moderatorRepository.save(moderator);
    }

    private void unassignRoleFromAdmin(String adminId, String projectId, String roleSlug) {
        Admin admin = adminRepository.findByNanoId(adminId)
                .orElseThrow(() -> new NotFoundException("Admin not found"));

        if (admin.getProjects() != null) {
            removeRoleFromProject(admin.getProjects(), projectId, roleSlug);
            adminRepository.save(admin);
        }
    }

    private void unassignRoleFromParticipant(String participantId, String projectId, String roleSlug) {
        Participant participant = participantRepository.findByNanoId(participantId)
                .orElseThrow(() -> new NotFoundException("Participant not found"));

        if (participant.getProjects() != null) {
            removeRoleFromProjectParticipant(participant.getProjects(), projectId, roleSlug);
            participantRepository.save(participant);
        }
    }

    private void unassignRoleFromModerator(String moderatorId, String projectId, String roleSlug) {
        Moderator moderator = moderatorRepository.findByNanoId(moderatorId)
                .orElseThrow(() -> new NotFoundException("Moderator not found"));

        if (moderator.getProjects() != null) {
            removeRoleFromProjectModerator(moderator.getProjects(), projectId, roleSlug);
            moderatorRepository.save(moderator);
        }
    }

    private void updateAdminProjectRole(List<Admin.ProjectRole> projects, String projectId, String roleSlug) {
        Admin.ProjectRole projectRole = projects.stream()
                .filter(p -> p.getProjectId().equals(projectId))
                .findFirst()
                .orElseGet(() -> {
                    Admin.ProjectRole newRole = new Admin.ProjectRole();
                    newRole.setProjectId(projectId);
                    newRole.setRoles(new ArrayList<>());
                    projects.add(newRole);
                    return newRole;
                });

        if (!projectRole.getRoles().contains(roleSlug)) {
            projectRole.getRoles().add(roleSlug);
        }
    }

    private void updateParticipantProjectRole(List<Participant.ProjectRole> projects, String projectId, String roleSlug) {
        Participant.ProjectRole projectRole = projects.stream()
                .filter(p -> p.getProjectId().equals(projectId))
                .findFirst()
                .orElseGet(() -> {
                    Participant.ProjectRole newRole = new Participant.ProjectRole();
                    newRole.setProjectId(projectId);
                    newRole.setRoles(new ArrayList<>());
                    projects.add(newRole);
                    return newRole;
                });

        if (!projectRole.getRoles().contains(roleSlug)) {
            projectRole.getRoles().add(roleSlug);
        }
    }

    private void updateModeratorProjectRole(List<Moderator.ProjectRole> projects, String projectId, String roleSlug) {
        Moderator.ProjectRole projectRole = projects.stream()
                .filter(p -> p.getProjectId().equals(projectId))
                .findFirst()
                .orElseGet(() -> {
                    Moderator.ProjectRole newRole = new Moderator.ProjectRole();
                    newRole.setProjectId(projectId);
                    newRole.setRoles(new ArrayList<>());
                    projects.add(newRole);
                    return newRole;
                });

        if (!projectRole.getRoles().contains(roleSlug)) {
            projectRole.getRoles().add(roleSlug);
        }
    }

    // Helper methods for unassigning roles (duplicated due to type erasure/different inner classes)
    private void removeRoleFromProject(List<Admin.ProjectRole> projects, String projectId, String roleSlug) {
        projects.stream()
                .filter(p -> p.getProjectId().equals(projectId))
                .findFirst()
                .ifPresent(projectRole -> {
                    projectRole.getRoles().remove(roleSlug);
                    if (projectRole.getRoles().isEmpty()) {
                        projects.remove(projectRole);
                    }
                });
    }

    private void removeRoleFromProjectParticipant(List<Participant.ProjectRole> projects, String projectId, String roleSlug) {
        projects.stream()
                .filter(p -> p.getProjectId().equals(projectId))
                .findFirst()
                .ifPresent(projectRole -> {
                    projectRole.getRoles().remove(roleSlug);
                    if (projectRole.getRoles().isEmpty()) {
                        projects.remove(projectRole);
                    }
                });
    }

    private void removeRoleFromProjectModerator(List<Moderator.ProjectRole> projects, String projectId, String roleSlug) {
        projects.stream()
                .filter(p -> p.getProjectId().equals(projectId))
                .findFirst()
                .ifPresent(projectRole -> {
                    projectRole.getRoles().remove(roleSlug);
                    if (projectRole.getRoles().isEmpty()) {
                        projects.remove(projectRole);
                    }
                });
    }
}