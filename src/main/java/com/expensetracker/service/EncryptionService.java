package com.expensetracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Service for encrypting and decrypting sensitive data like Plaid access tokens
 * Requirements: 17.1, 17.2
 */
@Service
public class EncryptionService {
    
    private static final String ALGORITHM = "AES";
    
    @Value("${encryption.secret:change-this-encryption-key-in-production}")
    private String encryptionSecret;
    
    /**
     * Encrypt a plain text string
     */
    public String encrypt(String plainText) {
        try {
            SecretKeySpec secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting data", e);
        }
    }
    
    /**
     * Decrypt an encrypted string
     */
    public String decrypt(String encryptedText) {
        try {
            SecretKeySpec secretKey = getSecretKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting data", e);
        }
    }
    
    /**
     * Generate a secret key from the encryption secret
     */
    private SecretKeySpec getSecretKey() {
        try {
            byte[] key = encryptionSecret.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // Use only first 128 bits
            return new SecretKeySpec(key, ALGORITHM);
        } catch (Exception e) {
            throw new RuntimeException("Error generating encryption key", e);
        }
    }
}
