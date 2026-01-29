package com.bracepl.reporting_service.domain.services;

import com.bracepl.reporting_service.adapter.in.entity.Admin;
import com.bracepl.reporting_service.adapter.out.interfaces.AdminRepository;
import com.bracepl.reporting_service.adapter.out.service.OtpClient;
import com.bracepl.reporting_service.application.dtos.AdminDto;
import com.bracepl.reporting_service.application.utils.CredentialSecretGenerator;
import com.bracepl.reporting_service.application.utils.NanoIdGenerator;
import com.bracepl.reporting_service.application.utils.PublicKeyGenerator;
import com.bracepl.reporting_service.application.utils.exceptions.ConflictException;
import com.bracepl.reporting_service.application.utils.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final NanoIdGenerator nanoIdGenerator;
//    private final PublicKeyGenerator publicKeyGenerator;
    private final OtpClient otpClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Create a new admin
     */
    @Transactional
    public AdminDto.Response createAdmin(AdminDto.CreateRequest request) {
        log.info("Creating admin with email: {}", request.getEmail());

        // Check if email already exists
        if (adminRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new ConflictException("Admin with this email already exists");
        }

        // Generate unique NanoID
        String nanoId = generateUniqueNanoId();
        String temporaryPassword = CredentialSecretGenerator.generateTemporaryPassword();
        otpClient.sendTemporaryPassword(request.getMobileNumber(), temporaryPassword, request.getEmail());
        String hashedPassword = passwordEncoder.encode(temporaryPassword);

        // Create admin entity
        Admin admin = Admin.builder()
                .publicKey(PublicKeyGenerator.generate())
                .id(nanoId)
                .email(request.getEmail().toLowerCase())
                .mobileNumber(request.getMobileNumber())
                .password(passwordEncoder.encode(hashedPassword))
                .status(true)
                .isVerified(false)
                .twoFactorEnabled(false)
                .isSuperAdmin(false)
                .build();

        Admin savedAdmin = adminRepository.save(admin);
        log.info("Admin created successfully with ID: {}", savedAdmin.getId());

        return mapToResponse(savedAdmin);
    }

    /**
     * Get all admins with pagination
     */
    public AdminDto.ListResponse getAllAdmins(int offset, int limit) {
        log.info("Fetching all admins (excluding super admin) with offset: {} and limit: {}", offset, limit);

        Pageable pageable = PageRequest.of(
                offset / limit,
                limit,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Admin> adminPage = adminRepository.findByIsSuperAdminFalse(pageable);

        return buildListResponse(adminPage, offset, limit);
    }


    /**
     * Get admins with filters
     */
    public AdminDto.ListResponse getAdminsWithFilters(AdminDto.FilterRequest filterRequest, 
                                                     int offset, int limit) {
        log.info("Fetching admins with filters: {}", filterRequest);

        Pageable pageable = PageRequest.of(offset / limit, limit, 
                Sort.by(Sort.Direction.DESC, "createdAt"));

        String search = filterRequest.getSearch();
        String createdAtStart = filterRequest.getCreatedAtStart();
        String createdAtEnd = filterRequest.getCreatedAtEnd();

        Page<Admin> adminPage;

        // Validate date range
        if (createdAtStart != null && !createdAtStart.isEmpty()) {
            if (createdAtEnd == null || createdAtEnd.isEmpty()) {
                throw new IllegalArgumentException("End date is required when start date is provided");
            }

            LocalDateTime startDate = parseDate(createdAtStart, true);
            LocalDateTime endDate = parseDate(createdAtEnd, false);

            if (search != null && !search.isEmpty()) {
                // Both search and date range
                adminPage = adminRepository.findWithFilters(search, startDate, endDate, pageable);
            } else {
                // Only date range
                adminPage = adminRepository.findByDateRange(startDate, endDate, pageable);
            }
        } else if (search != null && !search.isEmpty()) {
            // Only search
            adminPage = adminRepository.findBySearch(search, pageable);
        } else {
            // No filters, return all
            adminPage = adminRepository.findAll(pageable);
        }

        return buildListResponse(adminPage, offset, limit);
    }

    /**
     * Toggle admin status
     */
    @Transactional
    public AdminDto.Response toggleAdminStatus(String id) {
        log.info("Toggling status for admin ID: {}", id);

        Admin admin = findAdminById(id);
        admin.setStatus(!admin.getStatus());
        Admin updatedAdmin = adminRepository.save(admin);

        log.info("Admin status toggled successfully for ID: {}", id);
        return mapToResponse(updatedAdmin);
    }

    /**
     * Delete admin
     */
    @Transactional
    public void deleteAdmin(String id) {
        log.info("Deleting admin with ID: {}", id);

        Admin admin = findAdminById(id);
        adminRepository.delete(admin);

        log.info("Admin deleted successfully with ID: {}", id);
    }

    /**
     * Get total admin count
     */
    public long getTotalAdmins() {
        return adminRepository.count();
    }

    /**
     * Find admin by ID (internal use)
     */
    private Admin findAdminById(String id) {
        return adminRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Admin not found with ID: " + id));
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
        } while (adminRepository.existsById(nanoId));

        return nanoId;
    }

    /**
     * Parse date string to LocalDateTime
     */
    private LocalDateTime parseDate(String dateStr, boolean startOfDay) {
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            return startOfDay ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Expected: yyyy-MM-dd");
        }
    }

    /**
     * Map Admin entity to Response DTO
     */
    private AdminDto.Response mapToResponse(Admin admin) {
        return AdminDto.Response.builder()
                .id(admin.getMongoId())
                .publicKey(admin.getPublicKey())
                .firstname(admin.getFirstname())
                .lastname(admin.getLastname())
                .email(admin.getEmail())
                .mobileNumber(admin.getMobileNumber())
                .status(admin.getStatus())
                .isVerified(admin.getIsVerified())
                .twoFactorEnabled(admin.getTwoFactorEnabled())
                .createdAt(admin.getCreatedAt())
                .updatedAt(admin.getUpdatedAt())
                .build();
    }

    /**
     * Build list response with pagination info
     */
    private AdminDto.ListResponse buildListResponse(Page<Admin> adminPage, int offset, int limit) {
        List<AdminDto.Response> adminResponses = adminPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return AdminDto.ListResponse.builder()
                .admins(adminResponses)
                .total(adminPage.getTotalElements())
                .currentPage(offset / limit)
                .totalPages(adminPage.getTotalPages())
                .limit(limit)
                .build();
    }
}