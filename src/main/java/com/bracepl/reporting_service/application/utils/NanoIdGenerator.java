package com.bracepl.reporting_service.application.utils;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generates unique 8-character NanoIDs
 * Character set: 0-9, A-Z (uppercase only)
 */
@Component
public class NanoIdGenerator {
    
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int DEFAULT_SIZE = 8;
    private static final SecureRandom RANDOM = new SecureRandom();
    
    /**
     * Generate a NanoID with default size (8 characters)
     */
    public String generate() {
        return generate(DEFAULT_SIZE);
    }
    
    /**
     * Generate a NanoID with specified size
     */
    public String generate(int size) {
        StringBuilder id = new StringBuilder(size);
        
        for (int i = 0; i < size; i++) {
            int randomIndex = RANDOM.nextInt(ALPHABET.length());
            id.append(ALPHABET.charAt(randomIndex));
        }
        
        return id.toString();
    }
    
    /**
     * Validate if a string is a valid NanoID format
     */
    public boolean isValid(String id) {
        if (id == null || id.length() != DEFAULT_SIZE) {
            return false;
        }
        
        for (char c : id.toCharArray()) {
            if (ALPHABET.indexOf(c) == -1) {
                return false;
            }
        }
        
        return true;
    }
}