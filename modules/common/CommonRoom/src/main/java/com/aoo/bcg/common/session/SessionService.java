package com.aoo.bcg.common.session;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public final class SessionService {
    private final SessionStore store;
    private final Clock clock;
    private final Function<String, String> tokenHash;

    public SessionService(SessionStore store, Clock clock, Function<String, String> tokenHash) {
        this.store = Objects.requireNonNull(store); this.clock = Objects.requireNonNull(clock);
        this.tokenHash = Objects.requireNonNull(tokenHash);
    }

    public SessionRecord open(long userId, String deviceId, String refreshToken, Duration accessTtl, Duration refreshTtl) {
        store.revokeAll(userId, "REPLACED_BY_NEW_LOGIN");
        Instant now = clock.instant();
        SessionRecord record = new SessionRecord(UUID.randomUUID().toString(), userId, deviceId, 1,
                tokenHash.apply(refreshToken), now, now.plus(accessTtl), now.plus(refreshTtl), false);
        store.save(record, refreshTtl);
        return record;
    }

    public SessionRecord requireActive(String sessionId, String deviceId) {
        SessionRecord value = store.find(sessionId).orElseThrow(() -> new SecurityException("session not found"));
        if (value.revoked() || !value.deviceId().equals(deviceId) || !clock.instant().isBefore(value.refreshExpiresAt()))
            throw new SecurityException("session expired or device mismatch");
        return value;
    }

    public SessionRecord rotate(String sessionId, String deviceId, String oldRefreshToken,
                                String newRefreshToken, Duration accessTtl, Duration refreshTtl) {
        SessionRecord current = requireActive(sessionId, deviceId);
        if (!constantTimeEquals(current.refreshTokenHash(), tokenHash.apply(oldRefreshToken)))
            throw new SecurityException("refresh token mismatch");
        Instant now = clock.instant();
        SessionRecord next = new SessionRecord(UUID.randomUUID().toString(), current.userId(), deviceId,
                current.sessionVersion() + 1, tokenHash.apply(newRefreshToken), now,
                now.plus(accessTtl), now.plus(refreshTtl), false);
        if (!store.replaceIfVersion(current.userId(), current.sessionVersion(), next, refreshTtl))
            throw new SecurityException("refresh token already used");
        store.revoke(sessionId, "REFRESH_ROTATED");
        return next;
    }

    private static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null || left.length() != right.length()) return false;
        int diff = 0; for (int i = 0; i < left.length(); i++) diff |= left.charAt(i) ^ right.charAt(i);
        return diff == 0;
    }
}
