package com.doctrip.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Reversible AES-GCM encryption for Hospital Admin passwords — explicitly requested so DocTrip
 * Admin can view a hospital admin's current password (not just reset it blind), which a one-way
 * hash (as Story 1's patient passwords correctly use — see AuthService/PasswordEncoderConfig,
 * untouched by this) can never support by design.
 *
 * <p><b>Security trade-off, made deliberately and only for this one credential:</b> anyone with
 * backend process access or a copy of the database (plus this key) can recover every Hospital
 * Admin's plaintext password. That is fundamentally different from — and weaker than — password
 * hashing, which no key compromise can reverse. Patient login passwords are NOT changed by this;
 * only {@code hospital_admin.encrypted_password} uses it.
 *
 * <p>Key derivation: SHA-256 of {@code doctrip.jwt.secret} (reusing the existing JWT secret rather
 * than introducing a second one to configure) gives a stable 32-byte AES-256 key. Rotating
 * JWT_SECRET therefore also invalidates every stored encrypted password — same operational
 * consequence as it already has for outstanding JWTs.
 */
@Service
public class CredentialEncryptionService {

    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;

    public CredentialEncryptionService(@Value("${doctrip.jwt.secret}") String jwtSecret) {
        try {
            byte[] derived = MessageDigest.getInstance("SHA-256").digest(jwtSecret.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(derived, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Could not derive credential encryption key.", e);
        }
    }

    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt credential.", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Could not decrypt credential.", e);
        }
    }
}
