package com.bracepl.reporting_service.application.utils;

import com.bracepl.reporting_service.application.model.UserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT utility for token generation and verification
 * Algorithm: RS256
 * Expiry: configurable
 * Keys are automatically generated and stored in files if they don't exist
 */
@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${app.jwt.keys-directory:./jwt-keys}")
    private String keysDirectory;

    @Value("${app.jwt.expiration-days:7}")
    private int expirationDays;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    private static final String PRIVATE_KEY_FILE = "private_key.txt";
    private static final String PUBLIC_KEY_FILE = "public_key.txt";

    @PostConstruct
    public void init() throws Exception {
        log.info("Initializing JWT keys...");

        Path keysDir = Paths.get(keysDirectory);
        Path privateKeyPath = keysDir.resolve(PRIVATE_KEY_FILE);
        Path publicKeyPath = keysDir.resolve(PUBLIC_KEY_FILE);

        // Create directory if it doesn't exist
        if (!Files.exists(keysDir)) {
            Files.createDirectories(keysDir);
            log.info("Created keys directory: {}", keysDir.toAbsolutePath());
        }

        // Check if keys already exist
        if (Files.exists(privateKeyPath) && Files.exists(publicKeyPath)) {
            log.info("Loading existing keys from files...");
            loadKeysFromFiles(privateKeyPath, publicKeyPath);
        } else {
            log.info("Keys not found. Generating new RSA key pair...");
            generateAndSaveKeys(privateKeyPath, publicKeyPath);
        }

        log.info("JWT keys initialized successfully");
        log.info("Keys location: {}", keysDir.toAbsolutePath());
    }

    /**
     * Generate new RSA key pair and save to files
     */
    private void generateAndSaveKeys(Path privateKeyPath, Path publicKeyPath) throws Exception {
        // Generate 2048-bit RSA key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();

        this.privateKey = pair.getPrivate();
        this.publicKey = pair.getPublic();

        // Convert to Base64
        String privateKeyBase64 = Base64.getEncoder().encodeToString(privateKey.getEncoded());
        String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());

        // Save to files
        Files.writeString(privateKeyPath, privateKeyBase64);
        Files.writeString(publicKeyPath, publicKeyBase64);

        log.info("Generated and saved new RSA keys");
        log.info("Private key length: {} characters", privateKeyBase64.length());
        log.info("Public key length: {} characters", publicKeyBase64.length());
    }

    /**
     * Load existing keys from files
     */
    private void loadKeysFromFiles(Path privateKeyPath, Path publicKeyPath) throws Exception {
        try {
            // Read Base64 encoded keys from files
            String privateKeyBase64 = Files.readString(privateKeyPath).trim();
            String publicKeyBase64 = Files.readString(publicKeyPath).trim();

            log.info("Private key length: {} characters", privateKeyBase64.length());
            log.info("Public key length: {} characters", publicKeyBase64.length());

            // Decode and create keys
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            this.privateKey = keyFactory.generatePrivate(privateKeySpec);

            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            this.publicKey = keyFactory.generatePublic(publicKeySpec);

            log.info("Successfully loaded keys from files");
        } catch (Exception e) {
            log.error("Failed to load keys from files. Regenerating...", e);
            // If loading fails, regenerate
            generateAndSaveKeys(privateKeyPath, publicKeyPath);
        }
    }

    /**
     * Generate JWT token
     */
    public String generateToken(String userId, String clientPublicKey, UserType userType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userId);
        claims.put("public_key", clientPublicKey);
        claims.put("userType", userType.getValue());

        long expirationMillis = expirationDays * 24L * 60 * 60 * 1000;
        Date expirationDate = new Date(System.currentTimeMillis() + expirationMillis);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(expirationDate)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /**
     * Verify and parse JWT token
     */
    public Claims verifyToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractUserId(String token) {
        return verifyToken(token).get("id", String.class);
    }

    public String extractPublicKey(String token) {
        return verifyToken(token).get("public_key", String.class);
    }

    public UserType extractUserType(String token) {
        String value = verifyToken(token).get("userType", String.class);
        return UserType.fromValue(value);
    }

    public boolean isTokenExpired(String token) {
        try {
            return verifyToken(token).getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}