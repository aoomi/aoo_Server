package com.aoo.bcg.common.event;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryOutboxRepositoryTest {
    @Test void eventIsUniqueAndLeavesPendingSetAfterPublication() {
        InMemoryOutboxRepository repository = new InMemoryOutboxRepository();
        OutboxEvent event = new OutboxEvent("e1", "room", 1, "settled", "payload", Instant.now());
        repository.append(event);
        assertThrows(IllegalStateException.class, () -> repository.append(event));
        var claim=repository.claim(10,"worker",Instant.now(),java.time.Duration.ofSeconds(30)).getFirst();
        repository.markPublished(claim);
        assertTrue(repository.claim(10,"worker",Instant.now(),java.time.Duration.ofSeconds(30)).isEmpty());
    }

    @Test void failedEventWaitsUntilRetryTime() {
        InMemoryOutboxRepository repository = new InMemoryOutboxRepository();
        repository.append(new OutboxEvent("e2", "room", 2, "settled", "payload", Instant.now()));
        var claim=repository.claim(10,"worker",Instant.now(),java.time.Duration.ofSeconds(30)).getFirst();
        repository.recordFailure(claim,"temporary",Instant.now().plusSeconds(60),10);
        assertTrue(repository.claim(10,"worker",Instant.now(),java.time.Duration.ofSeconds(30)).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> repository.recordFailure(null,"error",Instant.now(),10));
    }

    @Test void expiredLeaseUsesFencingTokenAndDeadLettersAreObservable() {
        Instant now=Instant.parse("2026-08-23T00:00:00Z");
        InMemoryOutboxRepository repository=new InMemoryOutboxRepository();
        repository.append(new OutboxEvent("e3","room",3,"settled","payload",now));
        OutboxClaim stale=repository.claim(1,"same-worker",now,java.time.Duration.ofSeconds(1)).getFirst();
        OutboxClaim current=repository.claim(1,"same-worker",now.plusSeconds(2),java.time.Duration.ofSeconds(30)).getFirst();
        assertThrows(IllegalStateException.class,()->repository.markPublished(stale));
        assertTrue(repository.recordFailure(current,"fatal",now.plusSeconds(3),2));
        assertEquals(1,repository.deadLetterCount());
        assertEquals(1,repository.purgeTerminalBefore(now.plusSeconds(10),10));
    }
}
