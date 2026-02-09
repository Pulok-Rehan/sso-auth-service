package com.bracepl.reporting_service.configs;

import com.bracepl.reporting_service.adapter.in.entity.Admin;
import com.bracepl.reporting_service.adapter.out.interfaces.AdminRepository;
import com.bracepl.reporting_service.adapter.out.interfaces.ProjectRepository;
import com.bracepl.reporting_service.application.model.Project;
import com.bracepl.reporting_service.application.utils.NanoIdGenerator;
import com.bracepl.reporting_service.application.utils.PasswordHasher;
import com.bracepl.reporting_service.application.utils.PublicKeyGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SuperAdminInitializer {

    private final AdminRepository adminRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordHasher passwordHasher;
    private final NanoIdGenerator nanoIdGenerator;

    @Value("${app.superadmin.email}")
    private String superAdminEmail;

    @Value("${app.superadmin.projectSlug}")
    private String projectSlug;

    @Value("${app.superadmin.password}")
    private String superAdminPassword;

    @Value("${app.superadmin.projectSecret}")
    private String projectSecret;




    @Bean
    public CommandLineRunner initializeSuperAdmin() {
        return args -> {
            if (!adminRepository.existsByIsSuperAdminTrue()) {
                log.info("No SuperAdmin found. Creating one...");

                // 1. Create Default Project for SuperAdmin
                String projectSlug = "super-admin-project";
                Project project = projectRepository.findBySlug(projectSlug).orElse(null);
                
                String projectSecret = "superadminsecret"; // You might want to make this configurable too

                if (project == null) {
                    log.info("Creating default project for SuperAdmin...");
                    project = new Project();
                    project.setNanoId(nanoIdGenerator.generate()); // NanoID
                    project.setName("Super Admin Project");
                    project.setSlug(projectSlug);
                    project.setStatus(true);
                    project.setCreatedAt(LocalDateTime.now());
                    project.setUpdatedAt(LocalDateTime.now());
                    
                    // Create Credential
                    Project.Credential credential = new Project.Credential();
                    credential.setId(UUID.randomUUID().toString());
                    credential.setLabel("Default Key");
                    credential.setSecret(passwordHasher.hash(projectSecret));
                    credential.setCreatedAt(LocalDateTime.now());
                    credential.setUpdatedAt(LocalDateTime.now());
                    
                    project.setCredentials(new ArrayList<>(Collections.singletonList(credential)));
                    
                    // Add SuperAdmin Role
                    Project.AllowedRole role = new Project.AllowedRole();
                    role.setName("Super Admin");
                    role.setSlug("super-admin");
                    role.setUserType("Admin");
                    project.setAllowedRoles(new ArrayList<>(Collections.singletonList(role)));
                    
                    project = projectRepository.save(project);
                    log.info("Default project created. ID: {}, Secret: {}", project.getNanoId(), projectSecret);
                }

                // 2. Create SuperAdmin
                Admin.ProjectRole projectRole = new Admin.ProjectRole();
                projectRole.setProjectId(project.getId()); // Use NanoID
                projectRole.setRoles(new ArrayList<>(Collections.singletonList("super-admin")));

                Admin superAdmin = Admin.builder()
                        .nanoId(nanoIdGenerator.generate())
                        .publicKey(PublicKeyGenerator.generate())
                        .email(superAdminEmail)
                        .password(passwordEncoder.encode(superAdminPassword))
                        .isSuperAdmin(true)
                        .status(true)
                        .isVerified(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .projects(new ArrayList<>(Collections.singletonList(projectRole)))
                        .build();

                // Set createdBy for the project to this admin
                project.setCreatedBy(superAdmin.getNanoId());
                projectRepository.save(project);

                adminRepository.save(superAdmin);
                log.info("SuperAdmin created successfully with email: {}", superAdminEmail);
                log.info("Use Project ID: {} and Secret: {} for headers.", project.getNanoId(), projectSecret);
            } else {
                log.info("SuperAdmin already exists.");
            }
        };
    }
}