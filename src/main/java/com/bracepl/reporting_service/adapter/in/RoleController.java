package com.bracepl.reporting_service.adapter.in;

import com.bracepl.reporting_service.adapter.in.entity.Admin;
import com.bracepl.reporting_service.application.model.Project;
import com.bracepl.reporting_service.application.model.ServiceResponse;
import com.bracepl.reporting_service.application.model.UserType;
import com.bracepl.reporting_service.application.utils.exceptions.ConflictException;
import com.bracepl.reporting_service.application.utils.exceptions.NotFoundException;
import com.bracepl.reporting_service.application.utils.exceptions.UnauthorizedException;
import com.bracepl.reporting_service.domain.services.ProjectService;
import com.bracepl.reporting_service.domain.services.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/role")
@RequiredArgsConstructor
@Slf4j
public class RoleController {
    
    private final ProjectService projectService;
    private final RoleService roleService;

    @PostMapping("/{projectId}/roles")
    public ServiceResponse createRole(
            Authentication authentication,
            @PathVariable String projectId,
            @Valid @RequestBody RoleCreateRequest request) {
        
        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} creating role for project: {}", admin.getNanoId(), projectId);
            
            Project.AllowedRole role = projectService.createRole(projectId, request, admin);
            return new ServiceResponse("Role created successfully", role, "201");
        } catch (ConflictException e) {
            return new ServiceResponse(e.getMessage(), "409");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (UnauthorizedException e) {
            return new ServiceResponse(e.getMessage(), "401");
        } catch (Exception e) {
            log.error("Error creating role", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }
    
    /**
     * Get paginated roles with filters
     * POST /role/{projectId}/roles/listed/{offset}/{limit}
     */
    @PostMapping("/{projectId}/roles/listed/{offset}/{limit}")
    public ServiceResponse getRolesPaginated(
            Authentication authentication,
            @PathVariable String projectId,
            @PathVariable int offset,
            @PathVariable int limit,
            @RequestBody(required = false) RoleFilterRequest filterRequest) {
        
        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} fetching roles for project: {}", admin.getNanoId(), projectId);
            
            var project = projectService.getProject(projectId, admin);
            var roles = project.getAllowedRoles();
            
            // Apply filtering if needed
            if (filterRequest != null && filterRequest.getSearch() != null) {
                roles = roles.stream()
                        .filter(r -> r.getName().toLowerCase().contains(filterRequest.getSearch().toLowerCase()) ||
                                   r.getSlug().toLowerCase().contains(filterRequest.getSearch().toLowerCase()))
                        .toList();
            }
            
            // Simple pagination
            int start = Math.min(offset, roles.size());
            int end = Math.min(start + limit, roles.size());
            var paginatedRoles = roles.subList(start, end);
            
            Map<String, Object> data = new HashMap<>();
            data.put("roles", paginatedRoles);
            data.put("total", roles.size());
            
            return new ServiceResponse("Roles retrieved successfully", data, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (UnauthorizedException e) {
            return new ServiceResponse(e.getMessage(), "401");
        } catch (Exception e) {
            log.error("Error fetching roles", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }
    
    /**
     * Update role by slug
     * PUT /role/{projectId}/roles/{roleSlug}
     */
    @PutMapping("/{projectId}/roles/{roleSlug}")
    public ServiceResponse updateRole(
            Authentication authentication,
            @PathVariable String projectId,
            @PathVariable String roleSlug,
            @Valid @RequestBody RoleUpdateRequest request) {
        
        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} updating role {} in project: {}", admin.getNanoId(), roleSlug, projectId);
            
            Project.AllowedRole role = projectService.updateRole(projectId, roleSlug, request.getName(), admin);
            return new ServiceResponse("Role updated successfully", role, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (UnauthorizedException e) {
            return new ServiceResponse(e.getMessage(), "401");
        } catch (Exception e) {
            log.error("Error updating role", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }
    
    /**
     * Delete role by slug
     * DELETE /role/{projectId}/roles/{roleSlug}
     */
    @DeleteMapping("/{projectId}/roles/{roleSlug}")
    public ServiceResponse deleteRole(
            Authentication authentication,
            @PathVariable String projectId,
            @PathVariable String roleSlug) {
        
        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} deleting role {} from project: {}", admin.getNanoId(), roleSlug, projectId);
            
            projectService.deleteRole(projectId, roleSlug, admin);
            return new ServiceResponse("Role deleted successfully", null, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (UnauthorizedException e) {
            return new ServiceResponse(e.getMessage(), "401");
        } catch (Exception e) {
            log.error("Error deleting role", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Get all user types
     * GET /role/user-types
     */
    @GetMapping("/user-types")
    public ServiceResponse getAllUserTypes(Authentication authentication) {
        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} fetching all user types", admin.getNanoId());

            List<String> userTypes = Arrays.stream(UserType.values())
                    .filter(type -> type != UserType.SUPERADMIN)
                    .map(UserType::getValue)
                    .collect(Collectors.toList());

            return new ServiceResponse("User types retrieved successfully", userTypes, "200");
        } catch (Exception e) {
            log.error("Error fetching user types", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Get all roles for a project, optionally filtered by user type
     * GET /role/{projectId}/roles
     */
    @GetMapping("/{projectId}/roles")
    public ServiceResponse getAllRoles(
            Authentication authentication,
            @PathVariable String projectId,
            @RequestParam(required = false) String userId) {
        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} fetching roles for project: {}", admin.getNanoId(), projectId);

            var project = projectService.getProject(projectId, admin);
            var roles = project.getAllowedRoles();

            if (userId != null && !userId.isEmpty()) {
                UserType userType = roleService.getUserType(userId);
                roles = roles.stream()
                        .filter(role -> userType.getValue().equalsIgnoreCase(role.getUserType()))
                        .collect(Collectors.toList());
            }

            return new ServiceResponse("Roles retrieved successfully", roles, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (UnauthorizedException e) {
            return new ServiceResponse(e.getMessage(), "401");
        } catch (Exception e) {
            log.error("Error fetching all roles", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Assign role to user
     * POST /role/assign-role
     */
    @PostMapping("/assign-role")
    public ServiceResponse assignRole(
            Authentication authentication,
            @Valid @RequestBody AssignRoleRequest request) {
        try {
            Admin assigningAdmin = (Admin) authentication.getPrincipal();
            log.info("Admin {} assigning role {} to user {}", assigningAdmin.getNanoId(), request.getRole(), request.getUserId());

            roleService.assignRole(assigningAdmin, request.getUserId(), request.getUserType(), request.getProjectId(), request.getRole());

            return new ServiceResponse("Role assigned successfully", null, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (UnauthorizedException e) {
            return new ServiceResponse(e.getMessage(), "401");
        } catch (Exception e) {
            log.error("Error assigning role", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }
    
    // DTOs
    
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RoleCreateRequest {
        private String name;
        private String userType;
    }
    
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RoleUpdateRequest {
        private String name;
    }
    
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RoleFilterRequest {
        private String search;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AssignRoleRequest {
        private String userId;
        private String userType;
        private String projectId;
        private String role;
    }
}