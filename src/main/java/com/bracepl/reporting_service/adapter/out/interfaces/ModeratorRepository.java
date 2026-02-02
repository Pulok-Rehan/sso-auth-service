package com.bracepl.reporting_service.adapter.out.interfaces;

import com.bracepl.reporting_service.application.model.Moderator;
import com.bracepl.reporting_service.application.model.Participant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModeratorRepository extends MongoRepository<Moderator, String> {
    
    Optional<Moderator> findByEmail(String email);
    Optional<Moderator> findByNanoId(String email);
    Optional<Moderator> findByNanoIdInAndStatusTrue(String nanoId);

    Optional<Moderator> findByEmailAndStatusTrue(String email);
    Optional<Moderator> findByMobileNumberAndStatusTrue(String mobileNumber);

    Optional<Moderator> findByPublicKeyAndStatusTrue(String publicKey);
    
    Optional<Moderator> findByIdAndStatusTrue(String id);
    
    boolean existsByEmail(String email);
    
    boolean existsById(String id);
    default Optional<Moderator> findActiveByUsername(String username) {

        if (username.contains("@")) {
            return findByEmailAndStatusTrue(username);
        }

        return findByMobileNumberAndStatusTrue(username);
    }
}