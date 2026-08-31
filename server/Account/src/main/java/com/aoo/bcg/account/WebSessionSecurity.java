package com.aoo.bcg.account;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** HTTP/WSS edge policy for exact Origin, CSRF, hardened cookies and one-use socket tickets. */
public final class WebSessionSecurity {
    private static final Duration TICKET_TTL = Duration.ofSeconds(30);
    private final Set<String> allowedOrigins;
    private final Clock clock;
    private final SecureRandom random;
    private final Map<String, SocketTicket> tickets = new HashMap<>();

    public WebSessionSecurity(Set<String> allowedOrigins, Clock clock) {
        this(allowedOrigins, clock, new SecureRandom());
    }

    WebSessionSecurity(Set<String> allowedOrigins, Clock clock, SecureRandom random) {
        this.allowedOrigins = Set.copyOf(Objects.requireNonNull(allowedOrigins, "allowedOrigins"));
        if (this.allowedOrigins.isEmpty() || this.allowedOrigins.stream().anyMatch(v -> !isExactHttpsOrigin(v))) {
            throw new IllegalArgumentException("allowed origins must be exact HTTPS origins without a trailing slash");
        }
        this.clock = Objects.requireNonNull(clock, "clock");
        this.random = Objects.requireNonNull(random, "random");
    }

    public void authorizeMutation(String origin, String csrfCookie, String csrfHeader) {
        requireOrigin(origin);
        if (csrfCookie == null || csrfCookie.length() < 32 || !constantTimeEquals(csrfCookie, csrfHeader)) {
            throw new SecurityException("invalid CSRF proof");
        }
    }

    public String hardenedRefreshCookie(String refreshToken, Duration maxAge) {
        if (refreshToken == null || refreshToken.isBlank() || !refreshToken.matches("[A-Za-z0-9_-]{32,512}")
                || maxAge == null || maxAge.isNegative() || maxAge.isZero()) {
            throw new IllegalArgumentException("invalid refresh cookie");
        }
        return "__Host-refresh=" + refreshToken + "; Path=/; Max-Age=" + maxAge.toSeconds()
                + "; Secure; HttpOnly; SameSite=Strict";
    }

    public synchronized String issueSocketTicket(long accountId, String origin) {
        if (accountId <= 0) throw new IllegalArgumentException("invalid account");
        requireOrigin(origin);
        String value = randomValue();
        tickets.put(value, new SocketTicket(accountId, origin, clock.instant().plus(TICKET_TTL)));
        return value;
    }

    public synchronized long consumeSocketTicket(String value, String origin) {
        requireOrigin(origin);
        SocketTicket ticket = tickets.remove(value);
        if (ticket == null || !ticket.origin.equals(origin) || !clock.instant().isBefore(ticket.expiresAt)) {
            throw new SecurityException("invalid, expired or replayed socket ticket");
        }
        return ticket.accountId;
    }

    public synchronized void revokeAccountTickets(long accountId) {
        tickets.entrySet().removeIf(entry -> entry.getValue().accountId == accountId);
    }

    private void requireOrigin(String origin) {
        if (!allowedOrigins.contains(origin)) throw new SecurityException("origin is not allowed");
    }

    private static boolean isExactHttpsOrigin(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equals(uri.getScheme()) && uri.getRawAuthority() != null
                    && uri.getRawUserInfo() == null && uri.getRawPath().isEmpty()
                    && uri.getRawQuery() == null && uri.getRawFragment() == null;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }

    private String randomValue() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (right == null) return false;
        byte[] a = left.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] b = right.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(a, b);
    }

    private record SocketTicket(long accountId, String origin, Instant expiresAt) {}
}
