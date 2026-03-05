package com.bracepl.reporting_service.adapter.in;

import com.bracepl.reporting_service.adapter.in.entity.Admin;
import com.bracepl.reporting_service.application.dtos.AdminDto;
import com.bracepl.reporting_service.application.model.ServiceResponse;
import com.bracepl.reporting_service.application.utils.exceptions.*;
import com.bracepl.reporting_service.domain.services.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Admin Management Controller
 * All endpoints require Admin authentication
 */
@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    /**
     * Create new admin
     * POST /api/admin
     */
    @PostMapping
    public ServiceResponse createAdmin(
            Authentication authentication,
            @Valid @RequestBody AdminDto.CreateRequest request) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            if (!Boolean.TRUE.equals(admin.getIsSuperAdmin())) {
                return new ServiceResponse("You are not authorized to create admins", "401");
            }

            log.info("POST /api/admin - Creating admin with email: {}", request.getEmail());
            AdminDto.Response response = adminService.createAdmin(request);
            return new ServiceResponse("Admin created successfully", response, "201");
        } catch (ConflictException e) {
            return new ServiceResponse(e.getMessage(), "409");
        } catch (Exception e) {
            log.error("Error creating admin", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @GetMapping
    public ServiceResponse getAllAdmins(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "100") int limit) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            if (!Boolean.TRUE.equals(admin.getIsSuperAdmin())) {
                return new ServiceResponse("You are not authorized to view admins", "401");
            }

            log.info("GET /api/admin - Fetching admins with offset: {} and limit: {}", offset, limit);
            List<AdminDto.Response> response = adminService.getAllAdmins(offset, limit);
            return new ServiceResponse("Admins retrieved successfully", response, "200");
        } catch (Exception e) {
            log.error("Error fetching admins", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @PostMapping("/listed/{offset}/{limit}")
    public ServiceResponse getAdminsWithFilters(
            Authentication authentication,
            @PathVariable int offset,
            @PathVariable int limit,
            @RequestBody(required = false) AdminDto.FilterRequest filterRequest) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            if (!Boolean.TRUE.equals(admin.getIsSuperAdmin())) {
                return new ServiceResponse("You are not authorized to view admins", "401");
            }

            log.info("POST /api/admin/listed/{}/{} - Fetching admins with filters", offset, limit);

            // If no filter request, use empty filter
            if (filterRequest == null) {
                filterRequest = AdminDto.FilterRequest.builder().build();
            }

            AdminDto.ListResponse response = adminService.getAdminsWithFilters(filterRequest, offset, limit);
            return new ServiceResponse("Admins retrieved successfully", response, "200");
        } catch (IllegalArgumentException e) {
            return new ServiceResponse(e.getMessage(), "400");
        } catch (Exception e) {
            log.error("Error fetching admins with filters", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @PostMapping("/toggle-status/{id}")
    public ServiceResponse toggleAdminStatus(
            Authentication authentication,
            @PathVariable String id) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            if (!Boolean.TRUE.equals(admin.getIsSuperAdmin())) {
                return new ServiceResponse("You are not authorized to update admin status", "401");
            }

            log.info("POST /api/admin/toggle-status/{} - Toggling admin status", id);
            AdminDto.Response response = adminService.toggleAdminStatus(id);
            return new ServiceResponse("Admin status toggled successfully", response, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (Exception e) {
            log.error("Error toggling admin status", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @DeleteMapping("/{id}")
    public ServiceResponse deleteAdmin(
            Authentication authentication,
            @PathVariable String id) {

        try {
            Admin admin = (Admin) authentication.getPrincipal();
            if (!Boolean.TRUE.equals(admin.getIsSuperAdmin())) {
                return new ServiceResponse("You are not authorized to delete admins", "401");
            }

            log.info("DELETE /api/admin/{} - Deleting admin", id);
            adminService.deleteAdmin(id);
            return new ServiceResponse("Admin deleted successfully", null, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (Exception e) {
            log.error("Error deleting admin", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    @GetMapping("/analytics")
    public ServiceResponse getAnalytics(Authentication authentication) {
        try {
            Admin admin = (Admin) authentication.getPrincipal();
            // Analytics might be visible to all admins, but if strict superadmin only:
            // if (!Boolean.TRUE.equals(admin.getIsSuperAdmin())) {
            //    return new ServiceResponse("You are not authorized to view analytics", "401");
            // }
            // Leaving analytics open to all admins for now as it's usually dashboard info.
            
            log.info("GET /api/admin/analytics - Fetching analytics");
            AdminDto.AnalyticsResponse response = adminService.getAnalytics();
            return new ServiceResponse("Analytics retrieved successfully", response, "200");
        } catch (Exception e) {
            log.error("Error fetching analytics", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }

    /**
     * Lookup user by username and type
     * GET /api/admin/user-lookup
     */
    @GetMapping("/user-lookup")
    public ServiceResponse lookupUser(
            Authentication authentication,
            @RequestParam String username,
            @RequestParam String userType) {
        
        try {
            Admin admin = (Admin) authentication.getPrincipal();
            log.info("Admin {} looking up user: {} type: {}", admin.getNanoId(), username, userType);
            
            Map<String, String> userInfo = adminService.lookupUser(username, userType);
            return new ServiceResponse("User found", userInfo, "200");
        } catch (NotFoundException e) {
            return new ServiceResponse(e.getMessage(), "404");
        } catch (IllegalArgumentException e) {
            return new ServiceResponse(e.getMessage(), "400");
        } catch (Exception e) {
            log.error("Error looking up user", e);
            return new ServiceResponse("Something went wrong", "500");
        }
    }
}