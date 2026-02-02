package com.bracepl.reporting_service.domain.services;

import com.bracepl.reporting_service.adapter.out.interfaces.AuthSessionRepository;
import com.bracepl.reporting_service.adapter.out.interfaces.ProjectRepository;
import com.bracepl.reporting_service.application.dtos.CreateAuthSessionRequest;
import com.bracepl.reporting_service.application.dtos.CreateAuthSessionResponse;
import com.bracepl.reporting_service.application.model.AuthSession;
import com.bracepl.reporting_service.application.model.Project;
import com.bracepl.reporting_service.application.utils.CredentialSecretGenerator;
import com.bracepl.reporting_service.application.utils.NanoIdGenerator;
import com.bracepl.reporting_service.application.utils.PasswordHasher;
import com.bracepl.reporting_service.application.utils.exceptions.BadRequestException;
import com.bracepl.reporting_service.application.utils.exceptions.NotFoundException;
import com.bracepl.reporting_service.application.utils.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing auth sessions
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthSessionService {

    private final AuthSessionRepository authSessionRepository;
    private final ProjectRepository projectRepository;
    private final NanoIdGenerator nanoIdGenerator;
//    private final CredentialSecretGenerator credentialSecretGenerator;
    private final PasswordHasher passwordHasher;

    /**
     * Create a new auth session (Step 2 of authentication flow)
     * Requires valid project credentials
     */
    @Transactional
    public CreateAuthSessionResponse createAuthSession(CreateAuthSessionRequest request) {
        log.info("Creating auth session for project: {}", request.getProjectId());

        // 1. Validate and get project
        Project project = projectRepository.findByNanoIdAndStatusTrue(request.getProjectId())
                .orElseThrow(() -> new NotFoundException("Project not found or inactive"));

        // 2. Verify credential secret
        boolean credentialValid = project.getCredentials().stream()
                .anyMatch(credential -> passwordHasher.verify(request.getCredentialSecret(), credential.getSecret()));

        if (!credentialValid) {
            log.warn("Invalid credential secret for project: {}", request.getProjectId());
            throw new UnauthorizedException("Invalid credentials");
        }

        // 3. Verify role exists in project's allowed roles
        boolean roleExists = project.getAllowedRoles().stream()
//                .anyMatch(role -> role.getSlug().equals(request.getRole().toLowerCase()) && role.getUserType().equals(request.getUserType()));
                .anyMatch(role ->role.getUserType().equals(request.getUserType()));

//        if (!roleExists) {
//            log.warn("Role {} not found for user type {} in project: {}",
//                    request.getRole(), request.getUserType(), request.getProjectId());
//            throw new BadRequestException("Invalid role for this user type");
//        }

        // 4. Generate unique session ID
        String sessionId = generateUniqueSessionId();

        // 5. Generate session secret
        String plainSecret = CredentialSecretGenerator.generate();
        String hashedSecret = passwordHasher.hash(plainSecret);

        // 6. Create auth session
        AuthSession authSession = new AuthSession();
        authSession.setId(sessionId);
        authSession.setProjectId(request.getProjectId());
        authSession.setSecret(hashedSecret);
//        authSession.setRole(request.getRole().toLowerCase());
        authSession.setUserType(request.getUserType());
        authSession.setCreatedAt(LocalDateTime.now());
        authSession.setUpdatedAt(LocalDateTime.now());

        authSessionRepository.save(authSession);

        log.info("Auth session created successfully: {}", sessionId);

        // 7. Return response with plain secret (only time it's available)
        return new CreateAuthSessionResponse(
                        sessionId,
                        request.getProjectId(),
//                        request.getRole().toLowerCase(),
                        request.getUserType(),
                        plainSecret
                );
    }

    /**
     * Verify auth session credentials
     * Returns the matched session if valid
     */
    public AuthSession verifyAuthSession(String projectId, String sessionSecret) {
        log.debug("Verifying auth session for project: {}", projectId);

        // Get all sessions for this project
        List<AuthSession> sessions = authSessionRepository.findByProjectId(projectId);

        if (sessions.isEmpty()) {
            log.warn("No auth sessions found for project: {}", projectId);
            throw new UnauthorizedException("Invalid session");
        }

        // Find matching session
        for (AuthSession session : sessions) {
            if (passwordHasher.verify(sessionSecret, session.getSecret())) {
                log.debug("Auth session verified: {}", session.getId());
                return session;
            }
        }

        log.warn("Invalid session secret for project: {}", projectId);
        throw new UnauthorizedException("Invalid session");
    }

    /**
     * Delete expired auth sessions (older than 3 hours)
     * Called by scheduled job
     */
    @Transactional
    public void deleteExpiredSessions() {
        LocalDateTime threeHoursAgo = LocalDateTime.now().minusHours(3);
        authSessionRepository.deleteByCreatedAtBefore(threeHoursAgo);
        log.info("Deleted auth sessions older than 3 hours");
    }

    /**
     * Generate unique session ID
     */
    private String generateUniqueSessionId() {
        String id;
        do {
            id = nanoIdGenerator.generate();
        } while (authSessionRepository.existsById(id));
        return id;
    }
}