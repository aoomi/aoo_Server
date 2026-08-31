package com.aoo.bcg.account;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** PBKDF2 password storage with per-password salt and upgradeable parameters. */
public final class PasswordHasher {
    static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private final SecureRandom random;

    public PasswordHasher() {
        this(new SecureRandom());
    }

    PasswordHasher(SecureRandom random) {
        this.random = random;
    }

    public String hash(char[] password) {
        requirePassword(password);
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] derived = derive(password, salt, ITERATIONS);
        try {
            return "pbkdf2-sha256$" + ITERATIONS + "$"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(salt) + "$"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(derived);
        } finally {
            Arrays.fill(derived, (byte) 0);
        }
    }

    public boolean verify(char[] password, String encoded) {
        requirePassword(password);
        if (encoded == null || !encoded.startsWith("pbkdf2-sha256$")) return false;
        String[] parts = encoded.split("\\$", -1);
        if (parts.length != 4) return false;
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < ITERATIONS || iterations > 2_000_000) return false;
            byte[] salt = Base64.getUrlDecoder().decode(parts[2]);
            byte[] expected = Base64.getUrlDecoder().decode(parts[3]);
            byte[] actual = derive(password, salt, iterations);
            try {
                return MessageDigest.isEqual(expected, actual);
            } finally {
                Arrays.fill(actual, (byte) 0);
                Arrays.fill(expected, (byte) 0);
            }
        } catch (IllegalArgumentException invalidEncoding) {
            return false;
        }
    }

    /** Rejects plaintext or obsolete hashes at the persistence boundary. */
    public void requireModernEncoding(String encoded) {
        if (encoded == null || !encoded.startsWith("pbkdf2-sha256$")) {
            throw new IllegalArgumentException("plaintext and legacy password encodings are forbidden");
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException unavailable) {
            throw new IllegalStateException("PBKDF2WithHmacSHA256 is unavailable", unavailable);
        } finally {
            spec.clearPassword();
        }
    }

    private static void requirePassword(char[] password) {
        if (password == null || password.length < 10 || password.length > 256) {
            throw new IllegalArgumentException("password length must be between 10 and 256 characters");
        }
        byte[] bytes = new String(password).getBytes(StandardCharsets.UTF_8);
        try {
            if (bytes.length > 512) throw new IllegalArgumentException("password is too large");
        } finally {
            Arrays.fill(bytes, (byte) 0);
        }
    }
}
