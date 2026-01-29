package com.bracepl.reporting_service.adapter.out.interfaces;

import com.bracepl.reporting_service.adapter.in.entity.LoginAttempt;
import com.bracepl.reporting_service.application.model.Participant;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface LoginAttemptRepository
        extends MongoRepository<LoginAttempt, String> {

    default Optional<LoginAttempt> findActiveByUsername(String username, String mobileNumber) {
        Optional<LoginAttempt> loginAttempt = findByUsername(username);
        if (loginAttempt.isEmpty()) {
            return findByUsername(mobileNumber);
        }
        return loginAttempt;
    }

    Optional<LoginAttempt> findByUsername(String username);
}
