package com.bracepl.reporting_service.domain.services;

import com.bracepl.reporting_service.adapter.in.entity.LoginAttempt;
import com.bracepl.reporting_service.adapter.out.interfaces.LoginAttemptRepository;
import com.bracepl.reporting_service.application.utils.exceptions.NotFoundException;
import com.bracepl.reporting_service.application.utils.exceptions.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 4;

    private final LoginAttemptRepository repository;

    public LoginAttempt getOrCreate(String username) {
        return repository.findByUsername(username)
                .orElseGet(() -> repository.save(
                        LoginAttempt.builder()
                                .username(username)
                                .failedAttempts(0)
                                .blocked(false)
                                .firstLogin(true)
                                .build()
                ));
    }

    public boolean checkBlocked(LoginAttempt attempt) {
        if (attempt.isBlocked()) {
            return true;
        }
        return false;
    }

    public void loginFailed(LoginAttempt attempt) {
        attempt.setFailedAttempts(attempt.getFailedAttempts() + 1);
        attempt.setLastAttemptAt(LocalDateTime.now());

        if (attempt.getFailedAttempts() >= MAX_ATTEMPTS) {
            attempt.setBlocked(true);
            attempt.setBlockedAt(LocalDateTime.now());
        }
        repository.save(attempt);
    }

    public void loginSuccess(LoginAttempt attempt) {
        attempt.setFailedAttempts(0);
        attempt.setBlocked(false);
        attempt.setLastAttemptAt(LocalDateTime.now());
        // Do NOT automatically set firstLogin to false. 
        // This should be done only after password reset.
        repository.save(attempt);
    }

    public void unblock(String username, String mobileNumber) {
        LoginAttempt attempt = repository.findActiveByUsername(username, mobileNumber)
                .orElseThrow(() -> new NotFoundException("User not found"));

        attempt.setBlocked(false);
        attempt.setFailedAttempts(0);
        attempt.setBlockedAt(null);
        repository.save(attempt);
    }

    public void setFirstTimeLoginFalse(LoginAttempt loginAttempt){
        loginAttempt.setFirstLogin(false);
        repository.save(loginAttempt);
    }

    public void setFirstTimeLoginTrue(LoginAttempt loginAttempt){
        loginAttempt.setFirstLogin(true);
        repository.save(loginAttempt);
    }
}