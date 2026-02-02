package com.bracepl.reporting_service.adapter.in;

import com.bracepl.reporting_service.adapter.in.entity.Admin;
import com.bracepl.reporting_service.application.model.Project;
import com.bracepl.reporting_service.application.model.ServiceResponse;
import com.bracepl.reporting_service.application.utils.exceptions.UnauthorizedException;
import com.bracepl.reporting_service.domain.services.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Project management controller
 * All endpoints require admin authentication
 * Base path: /project
 */
@RestController
@RequestMapping("/project")
@RequiredArgsConstructor
@Slf4j
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public ServiceResponse createProject(
            Authentication authentication,
            @Valid @RequestBody ProjectCreateRequest request) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} creating project: {}", admin.getNanoId(), request.getName());

            Project project = projectService.createProject(request, admin);
            return new ServiceResponse("Project created successfully", project, "201");
        } catch (Exception e) {
            log.error("Error creating project", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @GetMapping
    public ServiceResponse getAllProjects(
            Authentication authentication) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} fetching all projects", admin.getNanoId());

            List<Project> projects = projectService.getAllProjects(admin);
            return new ServiceResponse("Projects retrieved successfully", projects, "200");
        } catch (Exception e) {
            log.error("Error fetching projects", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @GetMapping("/{id}")
    public ServiceResponse getProject(
            Authentication authentication,
            @PathVariable String id) {
        
        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} fetching project: {}", admin.getNanoId(), id);

            Project project = projectService.getProject(id, admin);
            return new ServiceResponse("Project retrieved successfully", project, "200");
        } catch (Exception e) {
            log.error("Error fetching project", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @PostMapping("/listed/{offset}/{limit}")
    public ServiceResponse getProjectsPaginated(
            Authentication authentication,
            @PathVariable int offset,
            @PathVariable int limit,
            @RequestBody(required = false) ProjectFilterRequest filterRequest) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} fetching projects page {}/{}", admin.getNanoId(), offset, limit);

            Page<Project> projectsPage = projectService.getProjectsPage(offset / limit, limit, admin);
            return new ServiceResponse("Projects retrieved successfully", projectsPage, "200");
        } catch (Exception e) {
            log.error("Error fetching paginated projects", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @PutMapping("/{id}")
    public ServiceResponse updateProject(
            Authentication authentication,
            @PathVariable String id,
            @Valid @RequestBody ProjectUpdateRequest request) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} updating project: {}", admin.getNanoId(), id);

            Project project = projectService.updateProject(id, request, admin);
            return new ServiceResponse("Project updated successfully", project, "200");
        } catch (Exception e) {
            log.error("Error updating project", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @PatchMapping("/status/{id}")
    public ServiceResponse toggleStatus(
            Authentication authentication,
            @PathVariable String id) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} toggling status for project: {}", admin.getNanoId(), id);

            Project project = projectService.toggleStatus(id, admin);
            return new ServiceResponse("Project status updated successfully", project, "200");
        } catch (UnauthorizedException e) {
            return new ServiceResponse(e.getMessage(), "401");
        } catch (Exception e) {
            log.error("Error toggling project status", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @DeleteMapping("/{id}")
    public ServiceResponse deleteProject(
            Authentication authentication,
            @PathVariable String id) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} deleting project: {}", admin.getNanoId(), id);

            projectService.deleteProject(id, admin);
            return new ServiceResponse("Project deleted successfully", null, "200");
        } catch (UnauthorizedException e) {
            return new ServiceResponse(e.getMessage(), "401");
        } catch (Exception e) {
            log.error("Error deleting project", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    // Nested DTOs for this controller

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProjectCreateRequest {
        private String name;
        private String secret;

        private Project.Services services;
        private Project.Callbacks callbacks;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProjectUpdateRequest {
        private String name;
        private String slug;

        private Project.Services services;
        private Project.Callbacks callbacks;
    }

    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ProjectFilterRequest {
        private String search;
        private String createdAtStart;
        private String createdAtEnd;
    }
}