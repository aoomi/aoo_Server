package com.aoo.bcg.admin;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/** Verifies one-time, independently authorized, short-lived four-eyes approval assertions. */
public final class AdminSecondaryApprovalVerifier {
    private static final String HMAC = "HmacSHA256";
    private final byte[] secret;
    private final AdminAuthorizationService authorization;
    private final Clock clock;
    private final AdminNonceStore nonces;

    public AdminSecondaryApprovalVerifier(String secret, AdminAuthorizationService authorization, Clock clock) {
        this(secret, authorization, clock, new InMemoryAdminNonceStore(clock));
    }

    public AdminSecondaryApprovalVerifier(String secret, AdminAuthorizationService authorization, Clock clock,
            AdminNonceStore nonces) {
        if (secret == null || secret.length() < 32) throw new IllegalArgumentException("approval secret too short");
        this.secret = secret.getBytes(StandardCharsets.UTF_8).clone();
        this.authorization = Objects.requireNonNull(authorization);
        this.clock = Objects.requireNonNull(clock);
        this.nonces = Objects.requireNonNull(nonces);
    }

    public boolean verify(long requesterId, String permission, String targetType, String targetId,
            String approvalId, String approverValue, String expiresValue, String signature) {
        if (approvalId == null || !approvalId.matches("[A-Za-z0-9_-]{16,128}")) return false;
        long approverId;
        Instant expiresAt;
        try {
            approverId = Long.parseLong(approverValue);
            expiresAt = Instant.ofEpochMilli(Long.parseLong(expiresValue));
        } catch (Exception error) { /* exception-policy: malformed approval is fail-closed */ return false; }
        Instant now = clock.instant();
        if (requesterId <= 0 || approverId <= 0 || requesterId == approverId
                || !expiresAt.isAfter(now) || expiresAt.isAfter(now.plusSeconds(300))) return false;
        String canonical = approvalId + "\n" + requesterId + "\n" + approverId + "\n"
                + permission + "\n" + targetType + "\n" + targetId + "\n" + expiresValue;
        if (!constantTime(sign(canonical), signature)
                || !authorization.allowed(approverId, permission, targetType, targetId)) return false;
        return nonces.consume("approval", approvalId, expiresAt);
    }

    String signForTest(String approvalId, long requesterId, long approverId, String permission,
            String targetType, String targetId, long expiresAt) {
        return sign(approvalId + "\n" + requesterId + "\n" + approverId + "\n" + permission
                + "\n" + targetType + "\n" + targetId + "\n" + expiresAt);
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC); mac.init(new SecretKeySpec(secret, HMAC));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) { throw new IllegalStateException("cannot sign secondary approval", error); }
    }
    private boolean constantTime(String left, String right) {
        return left != null && right != null && MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }
}
