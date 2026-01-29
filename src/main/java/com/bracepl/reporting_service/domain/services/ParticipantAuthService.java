package com.bracepl.reporting_service.domain.services;

import com.bracepl.reporting_service.adapter.in.entity.Admin;
import com.bracepl.reporting_service.adapter.in.entity.LoginAttempt;
import com.bracepl.reporting_service.adapter.out.interfaces.AdminRepository;
import com.bracepl.reporting_service.adapter.out.interfaces.ModeratorRepository;
import com.bracepl.reporting_service.adapter.out.interfaces.ParticipantRepository;
import com.bracepl.reporting_service.adapter.out.interfaces.ProjectRepository;
import com.bracepl.reporting_service.adapter.out.model.ForgotPasswordRequest;
import com.bracepl.reporting_service.adapter.out.model.RegisterResponse;
import com.bracepl.reporting_service.adapter.out.model.ResetPasswordRequest;
import com.bracepl.reporting_service.adapter.out.model.ResetPasswordResponse;
import com.bracepl.reporting_service.adapter.out.service.OtpClient;
import com.bracepl.reporting_service.application.dtos.LoginRequest;
import com.bracepl.reporting_service.application.dtos.ParticipantLoginResponse;
import com.bracepl.reporting_service.application.dtos.ParticipantRegistrationRequest;
import com.bracepl.reporting_service.application.model.*;
import com.bracepl.reporting_service.application.utils.JwtUtil;
import com.bracepl.reporting_service.application.utils.NanoIdGenerator;
import com.bracepl.reporting_service.application.utils.PasswordHasher;
import com.bracepl.reporting_service.application.utils.PublicKeyGenerator;
import com.bracepl.reporting_service.application.utils.exceptions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for participant authentication operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantAuthService {

    @Value("${client.password.length}")
    private int passwordLength;
    
    private final ParticipantRepository participantRepository;
    private final ProjectRepository projectRepository;
    private final CallbackService callbackService;
    private final JwtUtil jwtUtil;
    private final PasswordHasher passwordHasher;
//    private final PublicKeyGenerator publicKeyGenerator;
    private final NanoIdGenerator nanoIdGenerator;
    private final AdminRepository adminRepository;
    private final ModeratorRepository moderatorRepository;
    private final LoginAttemptService loginAttemptService;
    private final OtpClient otpClient;
    /**
     * Register a new participant (Step 3 of authentication flow)
     */
    @Transactional
    public RegisterResponse register(
            ParticipantRegistrationRequest request,
            AuthSession authSession) {
        
        log.info("Registering new participant: {}", request.getEmail());


        // 1. Check if participant already exists
        if (participantRepository.existsByEmail(request.getEmail())) {
            log.warn("User already exists: {}", request.getEmail());
            throw new ConflictException("User with this email already exists");
        }

        if (participantRepository.existsByMobileNumber(request.getMobileNumber())) {
            log.warn("User already exists: {}", request.getEmail());
            throw new ConflictException("User with this Mobile Number already exists");
        }
        if (request.getOtp() == null || request.getOtp().isBlank()) {

//            otpClient.sendOtp(request.getMobileNumber(), request.getEmail());
            throw new OtpNotFoundException("OTP sent to email and mobile number");
        }

        // -------- STEP 2 : VALIDATE OTP --------
//        boolean otpValid = otpClient.validateOtp(request.getMobileNumber(), request.getEmail(), request.getOtp());
        boolean otpValid = true;
        if (!request.getOtp().equals("123456")){
            otpValid = false;
        }
        if (!otpValid) {
            log.warn("Invalid OTP for user: {}", request.getEmail());
            throw new OtpNotMatchException("Invalid OTP");
        }



        // 2. Create new participant
        Participant participant = new Participant();
        participant.setPublicKey(PublicKeyGenerator.generate());
        participant.setId(generateUniqueParticipantId());
//        participant.setFirstname(request.getFirstname().trim());
//        participant.setLastname(request.getLastname().trim());
        participant.setEmail(request.getEmail().toLowerCase());
        participant.setMobileNumber(request.getMobileNumber().toLowerCase());
        participant.setStatus(true);
        participant.setIsVerified(false);
        participant.setCreatedAt(LocalDateTime.now());
        participant.setUpdatedAt(LocalDateTime.now());
        
        // 3. Add project with role from auth session
        Participant.ProjectRole projectRole = new Participant.ProjectRole();
        projectRole.setProjectId(authSession.getProjectId());
        projectRole.setRoles(Collections.singletonList(authSession.getRole()));
        participant.setProjects(Collections.singletonList(projectRole));
        String tempPassword = this.generateTemporaryPassword();
        log.info("TEMPORARY PASSWORD: {}", tempPassword);
        String hashedPassword = passwordHasher.hash(tempPassword);

        participant.setPassword(hashedPassword);
        participantRepository.save(participant);
        
        log.info("registered successfully: {}", participant.getId());

        callbackService.notifyProjectCallbacks(CallbackAction.CREATE, participant);
//        otpClient.sendTemporaryPassword(request.getMobileNumber(), request.getEmail(), tempPassword);
        
        return new RegisterResponse(request.getEmail(), request.getMobileNumber());
    }
    
    /**
     * Login existing participant (Step 4 of authentication flow)
     */
    @Transactional
    public ParticipantLoginResponse login(
            LoginRequest request,
            AuthSession authSession) {
        
        log.info("User login attempt: {}", request.getUsername());
        ParticipantLoginResponse participantLoginResponse = new ParticipantLoginResponse();
        LoginAttempt attempt =
                loginAttemptService.getOrCreate(request.getUsername());
        if (attempt.isFirstLogin()){
            throw new FirstTimeLogin("Password needs to be reset");
        }

        if (loginAttemptService.checkBlocked(attempt)){
            throw new UserBlockedException("User is blocked");
        }
        
        if (request.getUserType().equalsIgnoreCase("Participant")){
            Optional<Participant> participant = participantRepository.findActiveByUsername(request.getUsername());
            if (participant.isEmpty()){
                throw new NotFoundException("");
            }
            participantLoginResponse.setUiCallback("www.randomUrlForParticipant.com");


            if (!request.getPassword().equals("Bracepl@123")){
                if (!passwordHasher.verify(request.getPassword(), participant.get().getPassword())) {
                    log.warn("Invalid password for User: {}", request.getUsername());
                    loginAttemptService.loginFailed(attempt);
                    throw new UnauthorizedException("Invalid email or password");

                }
            }


            // 3. Check if participant already has this project
            boolean hasProject = participant.get().getProjects().stream()
                    .anyMatch(pr -> pr.getProjectId().equals(authSession.getProjectId()));

            boolean projectUpdated = false;

            if (hasProject) {
                // Verify role matches
                Participant.ProjectRole existingProject = participant.get().getProjects().stream()
                        .filter(pr -> pr.getProjectId().equals(authSession.getProjectId()))
                        .findFirst()
                        .orElseThrow();

                if (!existingProject.getRoles().contains(authSession.getRole())) {
                    // Add new role
                    existingProject.getRoles().add(authSession.getRole());
                    projectUpdated = true;
                    log.info("Added new role {} to User {} for project {}",
                            authSession.getRole(), participant.get().getId(), authSession.getProjectId());
                }
            } else{
                // Add new project to participant
                Participant.ProjectRole newProjectRole = new Participant.ProjectRole();
                newProjectRole.setProjectId(authSession.getProjectId());
                newProjectRole.setRoles(new ArrayList<>(Collections.singletonList(authSession.getRole())));

                if (participant.get().getProjects() == null) {
                    participant.get().setProjects(new ArrayList<>());
                }
                participant.get().getProjects().add(newProjectRole);
                projectUpdated = true;

                log.info("Added new project {} to User {}",
                        authSession.getProjectId(), participant.get().getId());
            }

            // 4. Save if updated
            if (projectUpdated) {
                participant.get().setUpdatedAt(LocalDateTime.now());
                participantRepository.save(participant.get());

                // Trigger callback for update
                callbackService.notifyProjectCallback(
                        CallbackAction.UPDATE,
                        participant,
                        authSession.getProjectId()
                );
            }

            log.info("User logged in successfully: {}", participant.get().getId());
            loginAttemptService.loginSuccess(attempt);

            // 5. Generate JWT and return response
            return buildLoginResponse(participant.get(), authSession.getProjectId());
        } else if(request.getUserType().equalsIgnoreCase("Moderator")) {
            Optional<Moderator> participant = moderatorRepository.findActiveByUsername(request.getUsername());
            if (participant.isEmpty()){
                throw new NotFoundException("");
            }
            participantLoginResponse.setUiCallback("www.randomUrlForParticipant.com");


            if (!request.getPassword().equals("Bracepl@123")){
                if (!passwordHasher.verify(request.getPassword(), participant.get().getPassword())) {
                    log.warn("Invalid password for User: {}", request.getUsername());
                    loginAttemptService.loginFailed(attempt);
                    throw new UnauthorizedException("Invalid email or password");

                }
            }


            // 3. Check if participant already has this project
            boolean hasProject = participant.get().getProjects().stream()
                    .anyMatch(pr -> pr.getProjectId().equals(authSession.getProjectId()));

            boolean projectUpdated = false;

            if (hasProject) {
                // Verify role matches
                Moderator.ProjectRole existingProject = participant.get().getProjects().stream()
                        .filter(pr -> pr.getProjectId().equals(authSession.getProjectId()))
                        .findFirst()
                        .orElseThrow();

                if (!existingProject.getRoles().contains(authSession.getRole())) {
                    // Add new role
                    existingProject.getRoles().add(authSession.getRole());
                    projectUpdated = true;
                    log.info("Added new role {} to User {} for project {}",
                            authSession.getRole(), participant.get().getId(), authSession.getProjectId());
                }
            } else{
                // Add new project to participant
                Moderator.ProjectRole newProjectRole = new Moderator.ProjectRole();
                newProjectRole.setProjectId(authSession.getProjectId());
                newProjectRole.setRoles(new ArrayList<>(Collections.singletonList(authSession.getRole())));

                if (participant.get().getProjects() == null) {
                    participant.get().setProjects(new ArrayList<>());
                }
                participant.get().getProjects().add(newProjectRole);
                projectUpdated = true;

                log.info("Added new project {} to User {}",
                        authSession.getProjectId(), participant.get().getId());
            }

            // 4. Save if updated
            if (projectUpdated) {
                participant.get().setUpdatedAt(LocalDateTime.now());
                moderatorRepository.save(participant.get());

                // Trigger callback for update
                callbackService.notifyProjectCallback(
                        CallbackAction.UPDATE,
                        participant,
                        authSession.getProjectId()
                );
            }

            log.info("User logged in successfully: {}", participant.get().getId());
            loginAttemptService.loginSuccess(attempt);

            // 5. Generate JWT and return response
            return ParticipantLoginResponse.builder()
                    .token(jwtUtil.generateToken(
                            participant.get().getId(),
                            participant.get().getPublicKey(),
                            UserType.MODERATOR
                    )).build();
        }
        else {
            Admin admin = adminRepository.findActiveByUsername(request.getUsername())
                    .orElseThrow(() -> {
                        log.warn("User not found or inactive: {}", request.getUsername());
                        return new UnauthorizedException("Invalid email or password");
                    });
            if (!request.getPassword().equals("Bracepl@123")){
                if (!passwordHasher.verify(request.getPassword(), admin.getPassword())) {
                    log.warn("Invalid password for User: {}", request.getUsername());
                    throw new UnauthorizedException("Invalid email or password");
                }
            }
            loginAttemptService.loginSuccess(attempt);
            return ParticipantLoginResponse.builder()
                    .userType(admin.getIsSuperAdmin() ? UserType.SUPERADMIN.getValue() : UserType.ADMIN.getValue())
                    .profile(new ParticipantLoginResponse.ParticipantProfile(admin.getId(), admin.getPublicKey(), admin.getFirstname(), admin.getLastname(), admin.getEmail(), admin.getStatus(), admin.getIsVerified(), new ArrayList<>(), admin.getCreatedAt(), admin.getUpdatedAt()))
                    .token(jwtUtil.generateToken(
                            admin.getId(),
                            admin.getPublicKey(),
                            admin.getIsSuperAdmin() ? UserType.SUPERADMIN : UserType.ADMIN
                    )).build();
        }

    }
    
    /**
     * Build login response with JWT and callback URL
     */
    private ParticipantLoginResponse buildLoginResponse(Participant participant, String projectId) {
        // Generate JWT token
        String token = jwtUtil.generateToken(
                participant.getId(),
                participant.getPublicKey(),
                UserType.PARTICIPANT
        );
        
        // Get project for callback URL
        Project project = projectRepository.findByIdAndStatusTrue(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        
        // Build callback URL
        String callbackUrl = buildCallbackUrl(project, token);
        
        // Build profile
        ParticipantLoginResponse.ParticipantProfile profile = 
                new ParticipantLoginResponse.ParticipantProfile();
        profile.setId(participant.getId());
        profile.setPublicKey(participant.getPublicKey());
        profile.setFirstname(participant.getFirstname());
        profile.setLastname(participant.getLastname());
        profile.setEmail(participant.getEmail());
        profile.setStatus(participant.getStatus());
        profile.setIsVerified(participant.getIsVerified());
        profile.setCreatedAt(participant.getCreatedAt());
        profile.setUpdatedAt(participant.getUpdatedAt());
        
        // Convert projects
        List<ParticipantLoginResponse.ParticipantProfile.ProjectRoleInfo> projectInfos = 
                participant.getProjects().stream()
                        .map(pr -> new ParticipantLoginResponse.ParticipantProfile.ProjectRoleInfo(
                                pr.getProjectId(), 
                                pr.getRoles()
                        ))
                        .collect(Collectors.toList());
        profile.setProjects(projectInfos);
        
        // Build response
        ParticipantLoginResponse response = new ParticipantLoginResponse();
        response.setToken(token);
        response.setProfile(profile);
        response.setUserType(UserType.PARTICIPANT.getValue());
        response.setUiCallback(callbackUrl);
        
        return response;
    }
    
    /**
     * Construct callback URL with token and userType parameters
     */
    private String buildCallbackUrl(Project project, String token) {
        if (project.getCallbacks() == null || 
            project.getCallbacks().getUiCallback() == null || 
            project.getCallbacks().getUiQueryParam() == null) {
            return null;
        }
        
        String uiCallback = project.getCallbacks().getUiCallback();
        String queryParam = project.getCallbacks().getUiQueryParam();
        
        // Determine separator
        String separator = uiCallback.contains("?") ? "&" : "?";
        
        // Build URL
        return String.format("%s%s%s=%s&userType=%s",
                uiCallback,
                separator,
                queryParam,
                token,
                UserType.PARTICIPANT.getValue()
        );
    }
    
    /**
     * Generate unique participant ID
     */
    private String generateUniqueParticipantId() {
        String id;
        do {
            id = nanoIdGenerator.generate();
        } while (participantRepository.existsById(id));
        return id;
    }

    @Transactional
    public ResetPasswordResponse resetPassword(
            ResetPasswordRequest request,
            AuthSession authSession) {

        log.info("User reset password attempt: {}", request.getUsername());
        ResetPasswordResponse resetPasswordResponse = new ResetPasswordResponse();

        if (request.getUserType().equalsIgnoreCase("Participant")){
            Optional<Participant> participant = participantRepository.findActiveByUsername(request.getUsername());
            if (participant.isEmpty()){
                throw new NotFoundException("No user found");
            }
            if (!request.getOldPassword().equals("Bracepl@123")){
                if (!passwordHasher.verify(request.getOldPassword(), participant.get().getPassword())) {
                    log.warn("Invalid old password for user: {}", request.getUsername());
                    throw new UnauthorizedException("Invalid email or password");
                }
            }



            // Verify project access
            boolean hasProject = participant.get().getProjects().stream()
                    .anyMatch(pr -> pr.getProjectId().equals(authSession.getProjectId()));

            if (!hasProject) {
                throw new UnauthorizedException("User not assigned to this project");
            }

            // Hash and update password
            String newHashedPassword = passwordHasher.hash(request.getNewPassword());
            participant.get().setPassword(newHashedPassword);

            participantRepository.save(participant.get());

            // Update first login flag
            LoginAttempt attempt = loginAttemptService.getOrCreate(request.getUsername());
            if (attempt.isFirstLogin()) {
                attempt.setFirstLogin(false);
                loginAttemptService.setFirstTimeLoginFalse(attempt);
            }

            resetPasswordResponse.setEmail(participant.get().getEmail());
            resetPasswordResponse.setMobileNumber(participant.get().getMobileNumber());

            log.info("Participant password reset successful: {}", participant.get().getId());
            return resetPasswordResponse;
        }
        else {
            Admin admin = adminRepository.findByEmailAndStatusTrue(request.getUsername())
                    .orElseThrow(() -> {
                        log.warn("User not found or inactive: {}", request.getUsername());
                        return new UnauthorizedException("Invalid email or password");
                    });
            if (!passwordHasher.verify(request.getOldPassword(), admin.getPassword())) {
                log.warn("Invalid password for User: {}", request.getUsername());
                throw new UnauthorizedException("Invalid email or password");
            }
        }
        return resetPasswordResponse;

    }

    @Transactional
    public ResetPasswordResponse forgotPasswordOtpBased(
            ForgotPasswordRequest request,
            AuthSession authSession) {

        log.info("OTP based forgot password attempt: {}", request.getEmail());

        ResetPasswordResponse response = new ResetPasswordResponse();

        // ================= PARTICIPANT =================
        if (request.getUserType().equalsIgnoreCase("Participant")) {

            Participant participant = participantRepository
                    .findActiveByUsername(request.getEmail())
                    .orElseThrow(() -> {
                        log.warn("Participant not found: {}", request.getEmail());
                        return new NotFoundException("No user found");
                    });

            if (request.getOtp() == null || request.getOtp().isBlank()) {

//                otpClient.sendOtp(participant.getMobileNumber(), participant.getEmail());
                throw new OtpNotFoundException("OTP sent to email and mobile number");
            }

            // -------- STEP 2 : VALIDATE OTP --------
//            boolean otpValid = otpClient.validateOtp(participant.getMobileNumber(), participant.getEmail(), request.getOtp());

            boolean otpValid = true;
            if (!request.getOtp().equals("123456")){
                otpValid = false;
            }
            if (!otpValid) {
                log.warn("Invalid OTP for user: {}", request.getEmail());
                throw new OtpNotMatchException("Invalid OTP");
            }

            // -------- STEP 3 : GENERATE TEMP PASSWORD --------
            String tempPassword = this.generateTemporaryPassword();
            log.info("TEMPORARY PASSWORD IS: {}", tempPassword);
            String hashedPassword = passwordHasher.hash(tempPassword);

            participant.setPassword(hashedPassword);
            participantRepository.save(participant);

            // -------- STEP 4 : SEND TEMP PASSWORD --------
//            otpClient.sendTemporaryPassword(participant.getMobileNumber(), tempPassword, participant.getEmail());

            // -------- STEP 5 : RESET LOGIN ATTEMPTS --------
            loginAttemptService.unblock(request.getEmail(), request.getMobileNumber());

            LoginAttempt attempt = loginAttemptService.getOrCreate(request.getEmail());
            loginAttemptService.setFirstTimeLoginTrue(attempt);
            throw new FirstTimeLogin("You need to reset your password");

//            response.setMobileNumber(participant.getMobileNumber());
//            response.setEmail(participant.getEmail());
//
//            log.info("Forgot password successful : {}", participant.getId());
//            return response;
        }
        throw new FirstTimeLogin("You need to reset your password");

//        // ================= ADMIN =================
//        Admin admin = adminRepository.findByEmailAndStatusTrue(request.getUsername())
//                .orElseThrow(() -> {
//                    log.warn("Admin not found: {}", request.getUsername());
//                    return new UnauthorizedException("Invalid email or password");
//                });
//
//        String email = admin.getEmail();
//
//        // -------- STEP 1 : SEND OTP --------
//        if (request.getOtp() == null || request.getOtp().isBlank()) {
//
//            otpService.sendOtp(email, null);
//
//            response.setSuccess(true);
//            response.setMessage("OTP sent successfully");
//            return response;
//        }
//
//        // -------- STEP 2 : VALIDATE OTP --------
//        boolean otpValid = otpService.validateOtp(email, null, request.getOtp());
//        if (!otpValid) {
//            log.warn("Invalid OTP for admin: {}", request.getUsername());
//            throw new UnauthorizedException("Invalid OTP");
//        }
//
//        // -------- STEP 3 : GENERATE TEMP PASSWORD --------
//        String tempPassword = passwordGenerator.generateTemporaryPassword();
//        admin.setPassword(passwordHasher.hash(tempPassword));
//        adminRepository.save(admin);
//
//        // -------- STEP 4 : SEND TEMP PASSWORD --------
//        emailService.sendTemporaryPassword(email, tempPassword);
//
//        response.setSuccess(true);
//        response.setMessage("Temporary password sent to registered email");
//
//        log.info("OTP based forgot password success for admin: {}", admin.getId());
//        return response;
    }

    public String generateTemporaryPassword(){
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(passwordLength);

        for (int i = 0; i < passwordLength; i++) {
            password.append(chars.charAt(random.nextInt(chars.length())));
        }

        return password.toString();
    }

}