package com.bracepl.reporting_service.application.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption/decryption for TOTP secrets
 */
@Component
public class AesEncryptor {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    
    @Value("${app.totp.secrets-key}")
    private String secretKeyBase64;
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    /**
     * Encrypt plain text using AES-256-GCM
     */
    public String encrypt(String plainText) throws Exception {
        SecretKey key = getKey();
        
        // Generate random IV
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        
        // Initialize cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);
        
        // Encrypt
        byte[] cipherText = cipher.doFinal(plainText.getBytes());
        
        // Combine IV + cipherText (includes auth tag)
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }
    
    /**
     * Decrypt cipher text using AES-256-GCM
     */
    public String decrypt(String encryptedText) throws Exception {
        SecretKey key = getKey();
        
        // Decode base64
        byte[] combined = Base64.getDecoder().decode(encryptedText);
        
        // Extract IV and cipher text
        byte[] iv = new byte[IV_LENGTH];
        byte[] cipherText = new byte[combined.length - IV_LENGTH];
        System.arraycopy(combined, 0, iv, 0, IV_LENGTH);
        System.arraycopy(combined, IV_LENGTH, cipherText, 0, cipherText.length);
        
        // Initialize cipher
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);
        
        // Decrypt
        byte[] plainText = cipher.doFinal(cipherText);
        
        return new String(plainText);
    }
    
    /**
     * Get secret key from base64-encoded string
     */
    private SecretKey getKey() {
        byte[] decodedKey = Base64.getDecoder().decode(secretKeyBase64);
        return new SecretKeySpec(decodedKey, "AES");
    }
}