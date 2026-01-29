package com.bracepl.reporting_service.application.utils;

import java.util.UUID;

/**
 * Public Key Generator Utility
 * Generates UUID v4 public keys for users (uppercase format)
 */
public class PublicKeyGenerator {

    // Private constructor to prevent instantiation
    private PublicKeyGenerator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Generate a new public key (UUID v4 in uppercase)
     *
     * @return A UUID v4 string in uppercase format
     */
    public static String generate() {
        return UUID.randomUUID().toString().toUpperCase();
    }

    /**
     * Validate if a string is a valid UUID format
     *
     * @param publicKey The public key to validate
     * @return true if valid UUID format, false otherwise
     */
    public static boolean isValid(String publicKey) {
        if (publicKey == null) {
            return false;
        }
        try {
            UUID.fromString(publicKey);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}