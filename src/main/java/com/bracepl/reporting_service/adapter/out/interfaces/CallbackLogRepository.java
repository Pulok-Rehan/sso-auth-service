package com.bracepl.reporting_service.adapter.out.interfaces;

import com.bracepl.reporting_service.application.model.CallbackLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallbackLogRepository extends MongoRepository<CallbackLog, String> {
}