package com.bracepl.reporting_service.adapter.out.interfaces;

import com.bracepl.reporting_service.application.model.Participant;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParticipantRepository extends MongoRepository<Participant, String> {
    
    Optional<Participant> findByEmail(String email);
    @Query("{ 'email': ?0, 'status': true }")
    Optional<Participant> findByEmailAndStatusTrue(String email);
    @Query("{ 'mobileNumber': ?0, 'status': true }")
    Optional<Participant> findByMobileNumberAndStatusTrue(String mobileNumber);

    default Optional<Participant> findActiveByUsername(String username) {

        if (username.contains("@")) {
            return findByEmailAndStatusTrue(username);
        }

        return findByMobileNumberAndStatusTrue(username);
    }



    Optional<Participant> findByPublicKeyAndStatusTrue(String publicKey);
    
    Optional<Participant> findByIdAndStatusTrue(String id);
    
    boolean existsByEmail(String email);
    boolean existsByMobileNumber(String mobileNumber);

    boolean existsById(String id);
}