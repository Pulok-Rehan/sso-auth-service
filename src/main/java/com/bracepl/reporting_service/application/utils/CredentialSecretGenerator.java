package com.bracepl.reporting_service.application.utils;

import java.security.SecureRandom;

/**
 * Credential Secret Generator Utility
 * Generates secure API credentials and auth session secrets
 * Character set: A-Z, a-z, 0-9
 * Default length: 32 characters
 */
public class CredentialSecretGenerator {

    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final int DEFAULT_LENGTH = 12;

    private static final SecureRandom RANDOM = new SecureRandom();

    // Private constructor to prevent instantiation
    private CredentialSecretGenerator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Generate a credential secret with default length (32 characters)
     *
     * @return A 32-character random string
     */
    public static String generate() {
        return generate(DEFAULT_LENGTH);
    }

    /**
     * Generate a credential secret with custom length
     *
     * @param length The desired length of the secret
     * @return A random string of specified length
     * @throws IllegalArgumentException if length <= 0
     */
    public static String generate(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }

        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);

        StringBuilder secret = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = Math.abs(bytes[i]) % ALPHABET.length();
            secret.append(ALPHABET.charAt(index));
        }

        return secret.toString();
    }

    /**
     * Generate a temporary password with specified length
     *
     * @param length The desired length of the password
     * @return A random password of specified length
     * @throws IllegalArgumentException if length <= 0
     */
    public static String generateTemporaryPassword(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Password length must be greater than 0");
        }

        StringBuilder password = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            password.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }

        return password.toString();
    }

    /**
     * Generate a temporary password with default length (12 characters)
     *
     * @return A 12-character random password
     */
    public static String generateTemporaryPassword() {
        return generateTemporaryPassword(6);
    }
}