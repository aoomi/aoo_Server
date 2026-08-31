package com.aoo.bcg.common.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;

class ScheduledOutboxRelayTest {
    @Test void publishesAndReportsBacklogWithoutManualPolling() throws Exception {
        Instant now = Instant.parse("2026-08-22T00:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        InMemoryOutboxRepository repository = new InMemoryOutboxRepository();
        repository.append(new OutboxEvent("event-1", "profile", 1,
                "GAME_PROFILE_ACTIVATED", java.util.Map.of("version", "1"), now));
        OutboxRelay relay = new OutboxRelay(repository, event -> {}, clock, Duration.ofSeconds(1));
        var health = new CopyOnWriteArrayList<ScheduledOutboxRelay.Health>();
        try (var scheduled = new ScheduledOutboxRelay(relay, repository, clock,
                Duration.ofMillis(10), 100, health::add)) {
            scheduled.start();
            long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
            while (health.isEmpty() && System.nanoTime() < deadline) Thread.sleep(5);
        }
        assertTrue(!health.isEmpty());
        assertEquals(1, health.getFirst().published());
        assertEquals(0, health.getFirst().backlog().pendingCount());
    }
}
