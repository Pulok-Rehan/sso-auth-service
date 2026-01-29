package com.bracepl.reporting_service.adapter.out.interfaces;

import com.bracepl.reporting_service.application.model.AuthSession;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuthSessionRepository extends MongoRepository<AuthSession, String> {
    
    List<AuthSession> findByProjectId(String projectId);
    
    void deleteByCreatedAtBefore(LocalDateTime dateTime);
    
    boolean existsById(String id);
}