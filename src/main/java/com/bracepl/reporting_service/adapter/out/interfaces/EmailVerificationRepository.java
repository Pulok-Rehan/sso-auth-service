package com.bracepl.reporting_service.adapter.out.interfaces;

import com.bracepl.reporting_service.adapter.in.entity.EmailVerification;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends MongoRepository<EmailVerification, String> {
    
    Optional<EmailVerification> findByEmailAndTokenAndUserType(String email, String token, String userType);
    Optional<EmailVerification> findByEmailAndUserType(String email, String userType);

    Optional<EmailVerification> findByEmail(String email);
    
    void deleteByCreatedAtBefore(LocalDateTime dateTime);
    
    void deleteByEmail(String email);
}