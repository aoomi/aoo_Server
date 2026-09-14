package com.aoo.bcg.common.event;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public record OutboxBacklog(long pendingCount, long failedCount, Optional<Instant> oldestCreatedAt) {
    public OutboxBacklog {
        if (pendingCount < 0 || failedCount < 0 || oldestCreatedAt == null)
            throw new IllegalArgumentException("invalid outbox backlog");
        if (pendingCount == 0 && oldestCreatedAt.isPresent())
            throw new IllegalArgumentException("empty backlog cannot have oldest event");
        if (pendingCount > 0 && oldestCreatedAt.isEmpty())
            throw new IllegalArgumentException("non-empty backlog requires oldest event");
    }

    public Duration oldestAge(Instant now) {
        return oldestCreatedAt.map(oldest -> Duration.between(oldest, now))
                .filter(age -> !age.isNegative()).orElse(Duration.ZERO);
    }
}
