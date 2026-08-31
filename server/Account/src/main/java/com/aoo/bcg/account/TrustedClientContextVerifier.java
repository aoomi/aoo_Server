package com.aoo.bcg.account;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Verifies gateway-attested device/channel/version data and rejects stale or replayed attestations. */
public final class TrustedClientContextVerifier {
    private final byte[] gatewayKey;
    private final Clock clock;
    private final Duration allowedSkew;
    private final ConcurrentHashMap<String, Instant> usedNonces = new ConcurrentHashMap<>();

    public TrustedClientContextVerifier(byte[] gatewayKey, Clock clock, Duration allowedSkew) {
        if (gatewayKey == null || gatewayKey.length < 32) throw new IllegalArgumentException("gateway key must contain at least 32 bytes");
        this.gatewayKey = gatewayKey.clone();
        this.clock = Objects.requireNonNull(clock, "clock");
        this.allowedSkew = Objects.requireNonNull(allowedSkew, "allowedSkew");
        if (allowedSkew.isNegative() || allowedSkew.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalArgumentException("allowed skew must be between zero and five minutes");
        }
    }

    public TrustedClientContext verify(SignedClientContext signed) {
        Objects.requireNonNull(signed, "signed");
        Instant now = clock.instant();
        Instant earliest = now.minus(allowedSkew);
        Instant latest = now.plus(allowedSkew);
        if (signed.issuedAt().isBefore(earliest) || signed.issuedAt().isAfter(latest)) {
            throw new SecurityException("client context is outside the clock-skew window");
        }
        byte[] expected = hmac(canonical(signed.context(), signed.issuedAt(), signed.nonce()));
        byte[] supplied;
        try {
            supplied = Base64.getUrlDecoder().decode(signed.signature());
        } catch (IllegalArgumentException invalid) {
            throw new SecurityException("invalid client context signature", invalid);
        }
        if (!MessageDigest.isEqual(expected, supplied)) throw new SecurityException("invalid client context signature");
        usedNonces.entrySet().removeIf(entry -> entry.getValue().isBefore(earliest));
        if (usedNonces.putIfAbsent(signed.nonce(), now) != null) throw new SecurityException("replayed client context");
        return signed.context();
    }

    /** Gateway-side helper; application callers should only use {@link #verify(SignedClientContext)}. */
    public SignedClientContext sign(TrustedClientContext context, Instant issuedAt, String nonce) {
        requireText(nonce, "nonce");
        return new SignedClientContext(context, issuedAt, nonce,
                Base64.getUrlEncoder().withoutPadding().encodeToString(hmac(canonical(context, issuedAt, nonce))));
    }

    private byte[] hmac(String input) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(gatewayKey, "HmacSHA256"));
            return mac.doFinal(input.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException unavailable) {
            throw new IllegalStateException("HmacSHA256 is unavailable", unavailable);
        }
    }

    private static String canonical(TrustedClientContext context, Instant issuedAt, String nonce) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(issuedAt, "issuedAt");
        requireText(nonce, "nonce");
        return context.deviceId() + '\n' + context.channel() + '\n' + context.clientVersion() + '\n'
                + context.ipAddress() + '\n' + issuedAt.toEpochMilli() + '\n' + nonce;
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank() || value.length() > 256 || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("invalid " + name);
        }
    }

    public record TrustedClientContext(String deviceId, String channel, String clientVersion, String ipAddress) {
        public TrustedClientContext {
            requireText(deviceId, "deviceId");
            requireText(channel, "channel");
            requireText(clientVersion, "clientVersion");
            requireText(ipAddress, "ipAddress");
        }
    }

    public record SignedClientContext(TrustedClientContext context, Instant issuedAt, String nonce, String signature) {
        public SignedClientContext {
            Objects.requireNonNull(context, "context");
            Objects.requireNonNull(issuedAt, "issuedAt");
            requireText(nonce, "nonce");
            requireText(signature, "signature");
        }
    }
}
