package com.bracepl.reporting_service.adapter.out.interfaces;

import com.bracepl.reporting_service.adapter.in.entity.Admin;
import com.bracepl.reporting_service.application.model.Moderator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends MongoRepository<Admin, String> {

    /**
     * Find admin by email
     */
    Optional<Admin> findByEmail(String email);
    Optional<Admin> findByEmailAndStatusTrue(String email);
    Optional<Admin> findByMobileNumberAndStatusTrue(String mobileNumber);



    /**
     * Find admin by public key
     */
    Optional<Admin> findByPublicKey(String publicKey);

    /**
     * Find admin by ID (NanoID)
     */
    Optional<Admin> findById(String id);
    Optional<Admin> findByNanoId(String id);

    /**
     * Find admin by public key and status
     */
    Optional<Admin> findByPublicKeyAndStatus(String publicKey, Boolean status);

    /**
     * Find admin by ID and status
     */
    Optional<Admin> findByIdAndStatus(String id, Boolean status);
    List<Admin> findByIsSuperAdminFalse();

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if ID exists
     */
    boolean existsById(String id);
    
    /**
     * Check if any SuperAdmin exists
     */
    boolean existsByIsSuperAdminTrue();

    /**
     * Find all admins with filters
     */
    @Query("{ $and: [ " +
            "{ $or: [ " +
            "  { 'firstname': { $regex: ?0, $options: 'i' } }, " +
            "  { 'lastname': { $regex: ?0, $options: 'i' } }, " +
            "  { 'email': { $regex: ?0, $options: 'i' } } " +
            "] }, " +
            "{ 'createdAt': { $gte: ?1, $lte: ?2 } } " +
            "] }")
    Page<Admin> findWithFilters(String search, LocalDateTime startDate,
                                LocalDateTime endDate, Pageable pageable);
    Page<Admin> findByIsSuperAdminFalse(Pageable pageable);

    /**
     * Find all admins with search only
     */
    @Query("{ $or: [ " +
            "{ 'firstname': { $regex: ?0, $options: 'i' } }, " +
            "{ 'lastname': { $regex: ?0, $options: 'i' } }, " +
            "{ 'email': { $regex: ?0, $options: 'i' } } " +
            "] }")
    Page<Admin> findBySearch(String search, Pageable pageable);

    /**
     * Find all admins with date range only
     */
    @Query("{ 'createdAt': { $gte: ?0, $lte: ?1 } }")
    Page<Admin> findByDateRange(LocalDateTime startDate, LocalDateTime endDate,
                                Pageable pageable);

    /**
     * Count total admins
     */
    long count();
    Optional<Admin> findByPublicKeyAndStatusTrue(String publicKey);

    default Optional<Admin> findActiveByUsername(String username) {

        if (username.contains("@")) {
            return findByEmailAndStatusTrue(username);
        }

        return findByMobileNumberAndStatusTrue(username);
    }
}