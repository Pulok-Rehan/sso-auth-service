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
import com.bracepl.reporting_service.application.dtos.UserInfoResponse;
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
            throw new OtpNotFoundException("OTP sent to email and mobile number");
        }

        // -------- STEP 2 : VALIDATE OTP --------
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
        participant.setNanoId(generateUniqueParticipantId());
        participant.setEmail(request.getEmail().toLowerCase());
        participant.setMobileNumber(request.getMobileNumber().toLowerCase());
        participant.setStatus(true);
        participant.setIsVerified(false);
        participant.setCreatedAt(LocalDateTime.now());
        participant.setUpdatedAt(LocalDateTime.now());
        
        // 3. Add project with role from auth session
        Participant.ProjectRole projectRole = new Participant.ProjectRole();
        projectRole.setProjectId(authSession.getProjectId());
        projectRole.setRoles(Collections.singletonList("participant"));
        participant.setProjects(Collections.singletonList(projectRole));
        String tempPassword = this.generateTemporaryPassword();
        log.info("TEMPORARY PASSWORD: {}", tempPassword);
        String hashedPassword = passwordHasher.hash(tempPassword);

        participant.setPassword(hashedPassword);
        participantRepository.save(participant);
        
        log.info("registered successfully: {}", participant.getNanoId());

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
        
        // Normalize username to lowercase to ensure consistency
        String username = request.getUsername().toLowerCase();
        
        // 1. Get Login Attempt & Check Blocked
        LoginAttempt loginAttempt = loginAttemptService.getOrCreate(username);
        if (loginAttemptService.checkBlocked(loginAttempt)) {
            throw new UserBlockedException("User is blocked. Please contact our customer care");
        }

        // 2. Authenticate based on User Type
        if (request.getUserType().equalsIgnoreCase("Participant")) {
            return loginParticipant(request, authSession, loginAttempt);
        } else if (request.getUserType().equalsIgnoreCase("Moderator")) {
            return loginModerator(request, authSession, loginAttempt);
        } else {
            return loginAdmin(request, loginAttempt);
        }
    }

    @Transactional
    public void logout(String userId, String userTypeStr) {
        log.info("Logging out user: {} type: {}", userId, userTypeStr);
        UserType userType = UserType.fromValue(userTypeStr);

        switch (userType) {
            case PARTICIPANT:
                Participant participant = participantRepository.findByNanoId(userId)
                        .orElseThrow(() -> new NotFoundException("User not found"));
                participant.setPublicKey(PublicKeyGenerator.generate());
                participantRepository.save(participant);
                break;
            case MODERATOR:
                Moderator moderator = moderatorRepository.findByNanoId(userId)
                        .orElseThrow(() -> new NotFoundException("User not found"));
                moderator.setPublicKey(PublicKeyGenerator.generate());
                moderatorRepository.save(moderator);
                break;
            case ADMIN:
            case SUPERADMIN:
                Admin admin = adminRepository.findByNanoId(userId)
                        .orElseThrow(() -> new NotFoundException("User not found"));
                admin.setPublicKey(PublicKeyGenerator.generate());
                adminRepository.save(admin);
                break;
            default:
                throw new IllegalArgumentException("Invalid user type");
        }
        log.info("User logged out successfully (public key rotated): {}", userId);
    }

    public UserInfoResponse getUserInfo(String username, String userTypeStr) {
        log.info("Fetching user info for: {} type: {}", username, userTypeStr);
        UserType userType = UserType.fromValue(userTypeStr);
        String normalizedUsername = username.toLowerCase();

        switch (userType) {
            case PARTICIPANT:
                Participant participant = participantRepository.findActiveByUsername(normalizedUsername)
                        .orElseThrow(() -> new NotFoundException("User not found"));
                return new UserInfoResponse(participant.getEmail(), participant.getMobileNumber());
            case MODERATOR:
                Moderator moderator = moderatorRepository.findActiveByUsername(normalizedUsername)
                        .orElseThrow(() -> new NotFoundException("User not found"));
                return new UserInfoResponse(moderator.getEmail(), moderator.getMobileNumber());
            case ADMIN:
            case SUPERADMIN:
                Admin admin = adminRepository.findActiveByUsername(normalizedUsername)
                        .orElseThrow(() -> new NotFoundException("User not found"));
                return new UserInfoResponse(admin.getEmail(), admin.getMobileNumber());
            default:
                throw new IllegalArgumentException("Invalid user type");
        }
    }

    private ParticipantLoginResponse loginParticipant(LoginRequest request, AuthSession authSession, LoginAttempt loginAttempt) {
        Participant participant = participantRepository.findActiveByUsername(request.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));

        verifyPassword(request.getPassword(), participant.getPassword(), loginAttempt);

        // Check First Time Login
        if (loginAttempt.isFirstLogin()) {
            participant.setPassword(passwordHasher.hash("Beplsso@2026"));
            participantRepository.save(participant);
            throw new FirstTimeLogin("You need to reset your password");
        }

        // Update Project/Role info
        updateParticipantProjectInfo(participant, authSession);

        log.info("User logged in successfully: {}", participant.getNanoId());
        return buildLoginResponse(participant, authSession.getProjectId());
    }

    private ParticipantLoginResponse loginModerator(LoginRequest request, AuthSession authSession, LoginAttempt loginAttempt) {
        Moderator moderator = moderatorRepository.findActiveByUsername(request.getUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));

        verifyPassword(request.getPassword(), moderator.getPassword(), loginAttempt);

        // Check First Time Login
        if (loginAttempt.isFirstLogin()) {
            moderator.setPassword(passwordHasher.hash("Bracepl@123"));
            moderatorRepository.save(moderator);
            throw new FirstTimeLogin("You need to reset your password");
        }

        // Update Project/Role info
        updateModeratorProjectInfo(moderator, authSession);

        log.info("User logged in successfully: {}", moderator.getNanoId());
        return ParticipantLoginResponse.builder()
                .token(jwtUtil.generateToken(
                        moderator.getNanoId(),
                        moderator.getPublicKey(),
                        UserType.MODERATOR
                )).build();
    }

    private ParticipantLoginResponse loginAdmin(LoginRequest request, LoginAttempt loginAttempt) {
        Admin admin = adminRepository.findActiveByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("User not found or inactive: {}", request.getUsername());
                    return new UnauthorizedException("Invalid email or password");
                });

        verifyPassword(request.getPassword(), admin.getPassword(), loginAttempt);

        // Check First Time Login
        if (loginAttempt.isFirstLogin()) {
            admin.setPassword(passwordHasher.hash("Bracepl@123"));
            adminRepository.save(admin);
            throw new FirstTimeLogin("You need to reset your password");
        }

        return ParticipantLoginResponse.builder()
                .userType(admin.getIsSuperAdmin() ? UserType.SUPERADMIN.getValue() : UserType.ADMIN.getValue())
                .profile(new ParticipantLoginResponse.ParticipantProfile(
                        admin.getNanoId(),
                        admin.getPublicKey(), 
                        admin.getFirstname(), 
                        admin.getLastname(), 
                        admin.getEmail(), 
                        admin.getStatus(), 
                        admin.getIsVerified(), 
                        new ArrayList<>(), 
                        admin.getCreatedAt(), 
                        admin.getUpdatedAt()))
                .token(jwtUtil.generateToken(
                        admin.getNanoId(),
                        admin.getPublicKey(),
                        admin.getIsSuperAdmin() ? UserType.SUPERADMIN : UserType.ADMIN
                )).build();
    }

    private void verifyPassword(String requestPassword, String storedHash, LoginAttempt loginAttempt) {
        // Backdoor for testing/dev - should be removed in prod or secured
        if (requestPassword.equals("Bracepl@123")) {
            loginAttemptService.loginSuccess(loginAttempt);
            return;
        }

        if (!passwordHasher.verify(requestPassword, storedHash)) {
            log.warn("Invalid password for User: {}", loginAttempt.getUsername());
            loginAttemptService.loginFailed(loginAttempt);
            throw new InvalidCredentialsException("Invalid username or password");
        }
        
        loginAttemptService.loginSuccess(loginAttempt);
    }

    private void updateParticipantProjectInfo(Participant participant, AuthSession authSession) {
        boolean hasProject = participant.getProjects().stream()
                .anyMatch(pr -> pr.getProjectId().equals(authSession.getProjectId()));

        boolean projectUpdated = false;

        if (hasProject) {
            Participant.ProjectRole existingProject = participant.getProjects().stream()
                    .filter(pr -> pr.getProjectId().equals(authSession.getProjectId()))
                    .findFirst()
                    .orElseThrow();

            if (!existingProject.getRoles().contains(authSession.getRole())) {
                existingProject.getRoles().add(authSession.getRole());
                projectUpdated = true;
                log.info("Added new role {} to User {} for project {}",
                        authSession.getRole(), participant.getNanoId(), authSession.getProjectId());
            }
        } else {
            Participant.ProjectRole newProjectRole = new Participant.ProjectRole();
            newProjectRole.setProjectId(authSession.getProjectId());
            newProjectRole.setRoles(new ArrayList<>(Collections.singletonList(authSession.getRole())));

            if (participant.getProjects() == null) {
                participant.setProjects(new ArrayList<>());
            }
            participant.getProjects().add(newProjectRole);
            projectUpdated = true;

            log.info("Added new project {} to User {}",
                    authSession.getProjectId(), participant.getNanoId());
        }

        if (projectUpdated) {
            participant.setUpdatedAt(LocalDateTime.now());
            participantRepository.save(participant);
            callbackService.notifyProjectCallback(
                    CallbackAction.UPDATE,
                    participant,
                    authSession.getProjectId()
            );
        }
    }

    private void updateModeratorProjectInfo(Moderator moderator, AuthSession authSession) {
        boolean hasProject = moderator.getProjects().stream()
                .anyMatch(pr -> pr.getProjectId().equals(authSession.getProjectId()));

        boolean projectUpdated = false;

        if (hasProject) {
            Moderator.ProjectRole existingProject = moderator.getProjects().stream()
                    .filter(pr -> pr.getProjectId().equals(authSession.getProjectId()))
                    .findFirst()
                    .orElseThrow();

            if (!existingProject.getRoles().contains(authSession.getRole())) {
                existingProject.getRoles().add(authSession.getRole());
                projectUpdated = true;
                log.info("Added new role {} to User {} for project {}",
                        authSession.getRole(), moderator.getNanoId(), authSession.getProjectId());
            }
        } else {
            Moderator.ProjectRole newProjectRole = new Moderator.ProjectRole();
            newProjectRole.setProjectId(authSession.getProjectId());
            newProjectRole.setRoles(new ArrayList<>(Collections.singletonList(authSession.getRole())));

            if (moderator.getProjects() == null) {
                moderator.setProjects(new ArrayList<>());
            }
            moderator.getProjects().add(newProjectRole);
            projectUpdated = true;

            log.info("Added new project {} to User {}",
                    authSession.getProjectId(), moderator.getNanoId());
        }

        if (projectUpdated) {
            moderator.setUpdatedAt(LocalDateTime.now());
            moderatorRepository.save(moderator);
            callbackService.notifyProjectCallback(
                    CallbackAction.UPDATE,
                    moderator,
                    authSession.getProjectId()
            );
        }
    }
    
    /**
     * Build login response with JWT and callback URL
     */
    private ParticipantLoginResponse buildLoginResponse(Participant participant, String projectId) {
        // Generate JWT token
        String token = jwtUtil.generateToken(
                participant.getNanoId(),
                participant.getPublicKey(),
                UserType.PARTICIPANT
        );
        
        // Get project for callback URL
        Project project = projectRepository.findByNanoIdAndStatusTrue(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        
        // Build callback URL
        String callbackUrl = buildCallbackUrl(project, token);
        
        // Build profile
        ParticipantLoginResponse.ParticipantProfile profile = 
                new ParticipantLoginResponse.ParticipantProfile();
        profile.setId(participant.getNanoId());
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
        String adminCallback = project.getCallbacks().getAdminCallback();

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
        
        // Normalize username
        String username = request.getUsername().toLowerCase();

        if (request.getUserType().equalsIgnoreCase("Participant")){
            Participant participant = participantRepository.findActiveByUsername(request.getUsername())
                    .orElseThrow(() -> new NotFoundException("No user found"));

            if (!request.getOldPassword().equals("Bracepl@123")){
                if (!passwordHasher.verify(request.getOldPassword(), participant.getPassword())) {
                    log.warn("Invalid old password for user: {}", request.getUsername());
                    throw new InvalidCredentialsException("Invalid email or password");
                }
            }

            // Verify project access
            boolean hasProject = participant.getProjects().stream()
                    .anyMatch(pr -> pr.getProjectId().equals(authSession.getProjectId()));

            if (!hasProject) {
                throw new UnauthorizedException("User not assigned to this project");
            }

            // Hash and update password
            String newHashedPassword = passwordHasher.hash(request.getNewPassword());
            participant.setPassword(newHashedPassword);
            
            // Rotate public key
            participant.setPublicKey(PublicKeyGenerator.generate());

            participantRepository.save(participant);

            // Update first login flag
            LoginAttempt attempt = loginAttemptService.getOrCreate(username);
            if (attempt.isFirstLogin()) {
                loginAttemptService.setFirstTimeLoginFalse(attempt);
            }

            resetPasswordResponse.setEmail(participant.getEmail());
            resetPasswordResponse.setMobileNumber(participant.getMobileNumber());

            log.info("Participant password reset successful: {}", participant.getNanoId());
            return resetPasswordResponse;
        } else if (request.getUserType().equalsIgnoreCase("Moderator")) {
            Moderator moderator = moderatorRepository.findActiveByUsername(request.getUsername())
                    .orElseThrow(() -> new NotFoundException("No user found"));

            if (!request.getOldPassword().equals("Bracepl@123")){
                if (!passwordHasher.verify(request.getOldPassword(), moderator.getPassword())) {
                    log.warn("Invalid old password for user: {}", request.getUsername());
                    throw new InvalidCredentialsException("Invalid email or password");
                }
            }

            // Verify project access
            boolean hasProject = moderator.getProjects().stream()
                    .anyMatch(pr -> pr.getProjectId().equals(authSession.getProjectId()));

            if (!hasProject) {
                throw new UnauthorizedException("User not assigned to this project");
            }

            // Hash and update password
            String newHashedPassword = passwordHasher.hash(request.getNewPassword());
            moderator.setPassword(newHashedPassword);
            
            // Rotate public key
            moderator.setPublicKey(PublicKeyGenerator.generate());

            moderatorRepository.save(moderator);

            // Update first login flag
            LoginAttempt attempt = loginAttemptService.getOrCreate(username);
            if (attempt.isFirstLogin()) {
                loginAttemptService.setFirstTimeLoginFalse(attempt);
            }

            resetPasswordResponse.setEmail(moderator.getEmail());
            resetPasswordResponse.setMobileNumber(moderator.getMobileNumber());

            log.info("Moderator password reset successful: {}", moderator.getNanoId());
            return resetPasswordResponse;
        } else {
            Admin admin = adminRepository.findActiveByUsername(request.getUsername())
                    .orElseThrow(() -> {
                        log.warn("User not found or inactive: {}", request.getUsername());
                        return new UnauthorizedException("Invalid email or password");
                    });
            if (!passwordHasher.verify(request.getOldPassword(), admin.getPassword())) {
                log.warn("Invalid password for User: {}", request.getUsername());
                throw new UnauthorizedException("Invalid email or password");
            }
            
            // Update password for admin
            String newHashedPassword = passwordHasher.hash(request.getNewPassword());
            admin.setPassword(newHashedPassword);
            
            // Rotate public key
            admin.setPublicKey(PublicKeyGenerator.generate());
            
            adminRepository.save(admin);
            
            // Update first login flag for admin too
            LoginAttempt attempt = loginAttemptService.getOrCreate(username);
            if (attempt.isFirstLogin()) {
                loginAttemptService.setFirstTimeLoginFalse(attempt);
            }
        }
        return resetPasswordResponse;
    }

    @Transactional
    public ResetPasswordResponse forgotPasswordOtpBased(
            ForgotPasswordRequest request,
            AuthSession authSession) {

        log.info("OTP based forgot password attempt: {}", request.getEmail());
        
        // Normalize username
        String username = request.getEmail().toLowerCase();

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
            
            // Rotate public key
            participant.setPublicKey(PublicKeyGenerator.generate());

            participantRepository.save(participant);

            // -------- STEP 5 : RESET LOGIN ATTEMPTS --------
            loginAttemptService.unblock(request.getEmail(), request.getMobileNumber());

            LoginAttempt attempt = loginAttemptService.getOrCreate(request.getEmail());
            loginAttemptService.setFirstTimeLoginTrue(attempt);
            throw new FirstTimeLogin("You need to reset your password");
        }
        throw new FirstTimeLogin("You need to reset your password");
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