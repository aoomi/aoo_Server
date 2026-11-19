package com.aoo.bcg.gateway;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Node-local ticket implementation. A distributed deployment must share this atomic store. */
public final class SecureWsTicketService implements WsTicketService {
    private static final Duration MAX_TTL = Duration.ofSeconds(30);
    private final ConcurrentHashMap<String, Ticket> tickets = new ConcurrentHashMap<>();
    private final SecureRandom random;
    private final Clock clock;

    public SecureWsTicketService(SecureRandom random, Clock clock) {
        this.random = Objects.requireNonNull(random);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public String issue(long userId, String deviceFingerprint, String allowedOrigin, Duration ttl) {
        if (userId <= 0 || blank(deviceFingerprint) || blank(allowedOrigin)
                || ttl == null || ttl.isZero() || ttl.isNegative() || ttl.compareTo(MAX_TTL) > 0) {
            throw new IllegalArgumentException("invalid websocket ticket request");
        }
        byte[] entropy = new byte[32];
        String value;
        do {
            random.nextBytes(entropy);
            value = Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
        } while (tickets.putIfAbsent(value, new Ticket(userId, deviceFingerprint, allowedOrigin,
                clock.instant().plus(ttl))) != null);
        return value;
    }

    @Override
    public ConnectionIdentity consumeOnce(String ticket, String origin) {
        return consume(ticket, origin, null, false);
    }

    @Override
    public ConnectionIdentity consumeOnce(String ticket, String origin, String deviceFingerprint) {
        return consume(ticket, origin, deviceFingerprint, true);
    }

    private ConnectionIdentity consume(String ticket, String origin, String deviceFingerprint, boolean verifyDevice) {
        if (blank(ticket) || blank(origin) || (verifyDevice && blank(deviceFingerprint))) throw new SecurityException("invalid websocket ticket");
        Ticket issued = tickets.remove(ticket);
        if (issued == null || !clock.instant().isBefore(issued.expiresAt())
                || !issued.allowedOrigin().equals(origin)
                || (verifyDevice && !issued.deviceFingerprint().equals(deviceFingerprint))) {
            throw new SecurityException("websocket ticket rejected");
        }
        return new ConnectionIdentity(issued.userId(), issued.deviceFingerprint(), issued.allowedOrigin(), "native:" + issued.deviceFingerprint());
    }

    private static boolean blank(String value) { return value == null || value.isBlank(); }

    private record Ticket(long userId, String deviceFingerprint, String allowedOrigin, Instant expiresAt) {}
}
