package com.aoo.bcg.common.event;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/** At-least-once outbox relay. Consumers must deduplicate by eventId. */
public final class OutboxRelay {
    private final OutboxRepository repository;
    private final OutboxPublisher publisher;
    private final Clock clock;
    private final Duration retryDelay;
    private final String workerId=java.util.UUID.randomUUID().toString();
    private final Duration claimLease=Duration.ofSeconds(30);
    private final int maxAttempts=10;

    public OutboxRelay(OutboxRepository repository, OutboxPublisher publisher, Clock clock,
            Duration retryDelay) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.retryDelay = Objects.requireNonNull(retryDelay, "retryDelay");
        if (retryDelay.isNegative() || retryDelay.isZero())
            throw new IllegalArgumentException("retryDelay must be positive");
    }

    public int publishBatch(int requestedLimit) {
        int limit = Math.max(1, Math.min(requestedLimit, 500));
        int published = 0;
        for (OutboxClaim claim : repository.claim(limit,workerId,clock.instant(),claimLease)) {
            try {
                publisher.publish(claim.event());
                repository.markPublished(claim);
                published++;
            } catch (Exception error) {
                repository.recordFailure(claim,error.getMessage(),clock.instant().plus(retryDelay),maxAttempts);
            }
        }
        return published;
    }
}
