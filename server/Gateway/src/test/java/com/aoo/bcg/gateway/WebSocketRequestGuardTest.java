package com.aoo.bcg.gateway;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketRequestGuardTest {
    private static final Instant NOW = Instant.parse("2026-08-22T10:00:00Z");

    @Test
    void acceptsOnlyContinuousBoundRequest() {
        WebSocketRequestGuard guard = new WebSocketRequestGuard(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(30));
        ConnectionSession session = new ConnectionSession("7", "100", 0, "pdk-v1", 0);
        WebSocketFrame frame = new WebSocketFrame("poker.paodekuai.play_cards_req", "req-1", 1,
                "100", 1, "pdk-v1", NOW.toEpochMilli(), Map.of());

        assertEquals(1, guard.validate(session, frame).lastSequence());
        ConnectionSession accepted = guard.validate(session, frame);
        assertThrows(IllegalArgumentException.class, () -> guard.validate(accepted, frame));
    }

    @Test
    void rejectsVersionRoomSequenceAndTimestampAttacks() {
        WebSocketRequestGuard guard = new WebSocketRequestGuard(Clock.fixed(NOW, ZoneOffset.UTC), Duration.ofSeconds(30));
        ConnectionSession session = new ConnectionSession("7", "100", 0, "pdk-v1", 0);
        assertThrows(SecurityException.class, () -> guard.validate(session, frame("101", "pdk-v1", 1, NOW, "a")));
        assertThrows(SecurityException.class, () -> guard.validate(session, frame("100", "pdk-v2", 1, NOW, "b")));
        assertThrows(IllegalArgumentException.class, () -> guard.validate(session, frame("100", "pdk-v1", 2, NOW, "c")));
        assertThrows(SecurityException.class, () -> guard.validate(session,
                frame("100", "pdk-v1", 1, NOW.minusSeconds(31), "d")));
    }

    @Test void derivesPresenceOnlyFromCurrentConnectionContext() {
        ConnectionPresenceRegistry presence = new ConnectionPresenceRegistry(Clock.fixed(NOW, ZoneOffset.UTC));
        ConnectionSession first = new ConnectionSession("7", "100", 0, "pdk-v1", 0, "connection-1", 1);
        ConnectionSession replacement = new ConnectionSession("7", "100", 0, "pdk-v1", 0, "connection-2", 2);
        presence.connected(first); presence.connected(replacement);
        assertFalse(presence.disconnected(first));
        assertEquals(ConnectionPresence.Status.ONLINE, presence.find("7", "100", 0).orElseThrow().status());
        assertTrue(presence.disconnected(replacement));
        assertEquals(ConnectionPresence.Status.OFFLINE, presence.find("7", "100", 0).orElseThrow().status());
    }

    @Test void allocatesMonotonicConnectionGenerations() {
        java.util.concurrent.atomic.AtomicInteger ids = new java.util.concurrent.atomic.AtomicInteger();
        ConnectionSessionFactory sessions = new ConnectionSessionFactory(new InMemoryConnectionGenerationStore(), () -> String.valueOf(ids.incrementAndGet()));
        ConnectionSession first = sessions.open("7", "100", 0, "v1", 0);
        ConnectionSession second = sessions.open("7", "100", 0, "v1", 3);
        ConnectionSession anotherSeat = sessions.open("7", "100", 1, "v1", 0);
        assertEquals(1, first.generation()); assertEquals(2, second.generation()); assertEquals(1, anotherSeat.generation());
        assertEquals(first.generation(), first.accept(1).generation());
    }

    private static WebSocketFrame frame(String room, String version, long seq, Instant time, String requestId) {
        return new WebSocketFrame("poker.paodekuai.play_cards_req", requestId, seq, room, 1,
                version, time.toEpochMilli(), Map.of());
    }
}
