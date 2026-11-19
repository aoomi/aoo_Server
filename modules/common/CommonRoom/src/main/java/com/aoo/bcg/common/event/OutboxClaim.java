package com.aoo.bcg.common.event;

import java.time.Instant;

public record OutboxClaim(OutboxEvent event, String workerId, String claimToken, Instant lockedUntil, int attempt) {
    public OutboxClaim {
        if (event == null || workerId == null || workerId.isBlank() || claimToken == null || claimToken.isBlank()
                || lockedUntil == null || attempt <= 0)
            throw new IllegalArgumentException("invalid outbox claim");
    }
}
