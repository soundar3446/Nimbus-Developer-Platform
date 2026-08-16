package com.nimbus.backend.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.util.Base64;

/**
 * JPA Attribute Converter to automatically encrypt and decrypt sensitive fields
 * (like OAuth tokens and registry passwords) at rest in the database.
 */
@Converter
public class StringCryptoConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES";
    
    // We expect a 16-byte (128-bit) encryption key from the environment.
    private static final byte[] KEY = resolveKey();

    private static byte[] resolveKey() {
        String envKey = System.getenv("NIMBUS_ENCRYPTION_KEY");
        if (StringUtils.hasText(envKey) && envKey.length() >= 16) {
            return envKey.substring(0, 16).getBytes(); // strictly 16 bytes for AES-128
        }
        // Fallback default key for local development. Do not use in production!
        return "NimbusDevKey1234".getBytes();
    }

    private static final Key SECRET_KEY = new SecretKeySpec(KEY, ALGORITHM);

    private static final ThreadLocal<Cipher> ENCRYPT_CIPHER = ThreadLocal.withInitial(() -> {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY);
            return cipher;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize encryption cipher", e);
        }
    });

    private static final ThreadLocal<Cipher> DECRYPT_CIPHER = ThreadLocal.withInitial(() -> {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY);
            return cipher;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize decryption cipher", e);
        }
    });

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (!StringUtils.hasText(attribute)) {
            return attribute;
        }
        try {
            byte[] encrypted = ENCRYPT_CIPHER.get().doFinal(attribute.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting field before database persistence.", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (!StringUtils.hasText(dbData)) {
            return dbData;
        }
        try {
            byte[] decrypted = DECRYPT_CIPHER.get().doFinal(Base64.getDecoder().decode(dbData));
            return new String(decrypted);
        } catch (Exception e) {
            // If decryption fails (e.g., trying to read old unencrypted data), 
            return dbData;
        }
    }
}
