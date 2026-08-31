package com.aoo.bcg.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** One-time verification-code authority with expiry, resend throttling and bounded attempts. */
public final class VerificationCodeService {
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final Duration RESEND_DELAY = Duration.ofSeconds(60);
    private static final int MAX_ATTEMPTS = 5;
    private final Clock clock;
    private final SecureRandom random;
    private final Delivery delivery;
    private final Map<Key, Challenge> challenges = new HashMap<>();

    public VerificationCodeService(Clock clock, Delivery delivery) {
        this(clock, new SecureRandom(), delivery);
    }

    VerificationCodeService(Clock clock, SecureRandom random, Delivery delivery) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.random = Objects.requireNonNull(random, "random");
        this.delivery = Objects.requireNonNull(delivery, "delivery");
    }

    public synchronized Instant issue(String destination, Purpose purpose) {
        Key key = new Key(normalize(destination), Objects.requireNonNull(purpose, "purpose"));
        Instant now = clock.instant();
        Challenge previous = challenges.get(key);
        if (previous != null && now.isBefore(previous.issuedAt.plus(RESEND_DELAY))) {
            throw new RateLimited("verification code resend is too frequent");
        }
        String code = "%06d".formatted(random.nextInt(1_000_000));
        Challenge challenge = new Challenge(digest(key, code), now, now.plus(TTL));
        challenges.put(key, challenge);
        try {
            delivery.deliver(key.destination, purpose, code, challenge.expiresAt);
        } catch (RuntimeException deliveryFailure) {
            challenges.remove(key);
            throw deliveryFailure;
        }
        return challenge.expiresAt;
    }

    public synchronized void consume(String destination, Purpose purpose, String code) {
        Key key = new Key(normalize(destination), Objects.requireNonNull(purpose, "purpose"));
        Challenge challenge = challenges.get(key);
        Instant now = clock.instant();
        if (challenge == null || !now.isBefore(challenge.expiresAt) || challenge.attempts >= MAX_ATTEMPTS) {
            challenges.remove(key);
            throw new InvalidCode("verification code is invalid or expired");
        }
        challenge.attempts++;
        if (!MessageDigest.isEqual(challenge.digest, digest(key, code))) {
            if (challenge.attempts >= MAX_ATTEMPTS) challenges.remove(key);
            throw new InvalidCode("verification code is invalid or expired");
        }
        challenges.remove(key);
    }

    private static byte[] digest(Key key, String code) {
        if (code == null || !code.matches("[0-9]{6}")) return new byte[32];
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest((key.destination + '\n' + key.purpose + '\n' + code).getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank() || value.length() > 254) throw new IllegalArgumentException("invalid destination");
        return value.strip().toLowerCase(java.util.Locale.ROOT);
    }

    public enum Purpose { REGISTER, LOGIN, CHANGE_PASSWORD, BIND_CHANNEL }

    @FunctionalInterface
    public interface Delivery {
        void deliver(String destination, Purpose purpose, String code, Instant expiresAt);
    }

    public static final class RateLimited extends IllegalStateException {
        public RateLimited(String message) { super(message); }
    }

    public static final class InvalidCode extends SecurityException {
        public InvalidCode(String message) { super(message); }
    }

    private record Key(String destination, Purpose purpose) {}
    private static final class Challenge {
        final byte[] digest;
        final Instant issuedAt;
        final Instant expiresAt;
        int attempts;
        Challenge(byte[] digest, Instant issuedAt, Instant expiresAt) {
            this.digest = digest;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
        }
    }
}
