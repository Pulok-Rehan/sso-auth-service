package com.bracepl.reporting_service.adapter.out.interfaces;

import com.bracepl.reporting_service.application.model.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends MongoRepository<Project, String> {
    
    Optional<Project> findBySlug(String slug);
    
    Optional<Project> findByIdAndStatusTrue(String id);
    Optional<Project> findById(String id);

    List<Project> findByIdInAndStatusTrue(List<String> ids);
    
    boolean existsBySlug(String slug);
    
    boolean existsById(String id);

    long countByStatusTrue();

    List<Project> findByCreatedBy(String createdBy);
    Page<Project> findByCreatedBy(String createdBy, Pageable pageable);
}