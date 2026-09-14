package com.aoo.bcg.common.session;

import java.time.Instant;

public record SessionRecord(String sessionId, long userId, String deviceId, long sessionVersion,
                            String refreshTokenHash, Instant issuedAt, Instant accessExpiresAt,
                            Instant refreshExpiresAt, boolean revoked) {
    public SessionRecord {
        if (sessionId == null || sessionId.isBlank() || userId <= 0 || deviceId == null || deviceId.isBlank())
            throw new IllegalArgumentException("invalid session identity");
        if (refreshTokenHash == null || refreshTokenHash.isBlank() || issuedAt == null
                || accessExpiresAt == null || refreshExpiresAt == null)
            throw new IllegalArgumentException("incomplete session");
    }
}
