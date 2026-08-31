package com.aoo.bcg.common.idempotency;

import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryIdempotencyStoreTest {
    @Test void preservesFirstResultForDuplicateRequest() {
        InMemoryIdempotencyStore<String> store = new InMemoryIdempotencyStore<>(Clock.systemUTC());
        var key=new IdempotencyKey("user","test.save",1,1,"request-1");store.save(key, "first", Duration.ofMinutes(1));
        store.save(key, "second", Duration.ofMinutes(1));
        assertEquals("first", store.find(key).orElseThrow());
    }

    @Test void reservesOneExecutorAndReleasesFailedExecution() {
        InMemoryIdempotencyStore<String> store = new InMemoryIdempotencyStore<>(Clock.systemUTC());
        var key=new IdempotencyKey("user","test.save",1,1,"request-2");assertTrue(store.acquire(key, Duration.ofMinutes(1)));
        assertFalse(store.acquire(key, Duration.ofMinutes(1)));
        assertTrue(store.find(key).isEmpty());
        store.release(key);
        assertTrue(store.acquire(key, Duration.ofMinutes(1)));
        store.save(key, "done", Duration.ofMinutes(1));
        assertEquals("done", store.find(key).orElseThrow());
        store.release(key);
        assertEquals("done", store.find(key).orElseThrow());
    }
}
