package com.bracepl.reporting_service.adapter.out.interfaces;

import com.bracepl.reporting_service.adapter.in.entity.Script;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScriptRepository extends MongoRepository<Script, String> {
    
    Optional<Script> findByName(String name);
}