package com.bracepl.reporting_service.application.utils;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.stereotype.Component;

/**
 * Password hashing utility using BCrypt
 * Salt rounds: 10
 */
@Component
public class PasswordHasher {
    
    private static final int SALT_ROUNDS = 10;
    
    /**
     * Hash a password using BCrypt
     */
    public String hash(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(SALT_ROUNDS));
    }
    
    /**
     * Verify a password against its hash
     */
    public boolean verify(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (Exception e) {
            return false;
        }
    }
}