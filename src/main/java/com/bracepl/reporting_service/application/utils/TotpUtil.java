package com.bracepl.reporting_service.application.utils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

/**
 * TOTP (Time-based One-Time Password) utility for 2FA
 * Algorithm: SHA1, 6 digits, 30-second window
 */
@Component
public class TotpUtil {
    
    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final TimeProvider timeProvider = new SystemTimeProvider();
    private final CodeGenerator codeGenerator = new DefaultCodeGenerator(HashingAlgorithm.SHA1);
    private final CodeVerifier codeVerifier;
    
    public TotpUtil() {
        // 30-second time window, allow 1 window before/after (90 seconds total)
        DefaultCodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
        verifier.setTimePeriod(30);
        verifier.setAllowedTimePeriodDiscrepancy(1);
        this.codeVerifier = verifier;
    }
    
    /**
     * Generate a new TOTP secret
     */
    public String generateSecret() {
        return secretGenerator.generate();
    }
    
    /**
     * Generate QR code as base64 data URL
     */
    public String generateQrCode(String secret, String email) throws Exception {
        QrDataFactory qrDataFactory = new QrDataFactory(HashingAlgorithm.SHA256, 1, 1);
        QrData data = qrDataFactory.newBuilder()
                .label(email)
                .secret(secret)
                .issuer("Brac SSO")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();
        
        String qrCodeData = data.getUri();
        
        // Generate QR code image
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeData, BarcodeFormat.QR_CODE, 300, 300);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        String base64Image = Base64.getEncoder().encodeToString(outputStream.toByteArray());
        
        return "data:image/png;base64," + base64Image;
    }
    
    /**
     * Verify TOTP code
     */
    public boolean verifyCode(String secret, String code) {
        return codeVerifier.isValidCode(secret, code);
    }
    
    /**
     * Get current time step for replay protection
     */
    public long getCurrentTimeStep() {
        long currentTimeMillis = timeProvider.getTime();
        return Math.floorDiv(currentTimeMillis, 30000); // 30 seconds in milliseconds
    }
    
    /**
     * Generate backup codes (10 codes, 10 characters each, alphanumeric uppercase)
     */
    public String[] generateBackupCodes() {
        String[] codes = new String[10];
        for (int i = 0; i < 10; i++) {
            codes[i] = generateBackupCode();
        }
        return codes;
    }
    
    /**
     * Generate single backup code
     */
    private String generateBackupCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            int index = (int) (Math.random() * chars.length());
            code.append(chars.charAt(index));
        }
        return code.toString();
    }
}