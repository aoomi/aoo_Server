package com.aoo.bcg.admin;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

/**
 * Authenticates an administrator independently from all player credentials.
 * The signed identity prevents a caller that only controls X-Admin-Id from
 * impersonating another operator and the nonce makes captured requests one-shot.
 */
public final class AdminRequestAuthenticator {
    private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(2);
    private static final String HMAC = "HmacSHA256";
    private final byte[] secret;
    private final Clock clock;
    private final AdminNonceStore nonces;

    public AdminRequestAuthenticator(String secret, Clock clock) {
        this(secret, clock, new InMemoryAdminNonceStore(clock));
    }

    public AdminRequestAuthenticator(String secret, Clock clock, AdminNonceStore nonces) {
        Objects.requireNonNull(secret, "secret");
        if (secret.length() < 32) throw new IllegalArgumentException("admin credential secret is too short");
        this.secret = secret.getBytes(StandardCharsets.UTF_8).clone();
        this.clock = Objects.requireNonNull(clock, "clock");
        this.nonces = Objects.requireNonNull(nonces, "nonces");
    }

    public long authenticate(String authorization, String operator, String timestamp,
            String nonce, String signature, String requestId, String method, String path,
            Map<String, String> headers) {
        rejectPlayerCredentialHeaders(headers);
        if (!constantTimeEquals("Bearer " + new String(secret, StandardCharsets.UTF_8), authorization)) {
            throw new SecurityException("invalid admin bearer credential");
        }
        long operatorId = positiveLong(operator, "operator");
        Instant issuedAt;
        try { issuedAt = Instant.ofEpochMilli(Long.parseLong(timestamp)); }
        catch (Exception error) { throw new SecurityException("invalid admin credential timestamp"); }
        Instant now = clock.instant();
        if (issuedAt.isBefore(now.minus(MAX_CLOCK_SKEW)) || issuedAt.isAfter(now.plus(MAX_CLOCK_SKEW))) {
            throw new SecurityException("expired admin credential");
        }
        if (nonce == null || !nonce.matches("[A-Za-z0-9_-]{16,128}")) {
            throw new SecurityException("invalid admin credential nonce");
        }
        String canonical = operatorId + "\n" + timestamp + "\n" + nonce + "\n"
                + requestId + "\n" + method + "\n" + path;
        if (!constantTimeEquals(sign(canonical), signature)) {
            throw new SecurityException("invalid admin credential signature");
        }
        if (!nonces.consume("request", nonce, now.plus(MAX_CLOCK_SKEW))) {
            throw new SecurityException("replayed admin credential");
        }
        return operatorId;
    }

    public String signForTest(long operatorId, long timestamp, String nonce,
            String requestId, String method, String path) {
        return sign(operatorId + "\n" + timestamp + "\n" + nonce + "\n"
                + requestId + "\n" + method + "\n" + path);
    }

    public static void requireLoopbackBoundary(InetSocketAddress bindAddress) {
        InetAddress address = Objects.requireNonNull(bindAddress, "bindAddress").getAddress();
        if (address == null || !address.isLoopbackAddress()) {
            throw new IllegalStateException("AdminApi must bind to a private loopback control-plane address");
        }
    }

    private void rejectPlayerCredentialHeaders(Map<String, String> headers) {
        for (String name : headers.keySet()) {
            String normalized = name.toLowerCase(java.util.Locale.ROOT);
            if (normalized.startsWith("x-player-") || normalized.equals("x-user-token")
                    || normalized.equals("x-session-id")) {
                throw new SecurityException("player credentials are forbidden on the admin control plane");
            }
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret, HMAC));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new IllegalStateException("cannot sign admin credential", error);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) return false;
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    private long positiveLong(String value, String field) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed > 0) return parsed;
        } catch (Exception ignored) { }
        throw new SecurityException("invalid admin " + field);
    }
}
