package com.aoo.bcg.gateway;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** New-protocol source of truth for online/offline state; stale transport callbacks cannot overwrite it. */
public final class ConnectionPresenceRegistry {
    private record Key(String userId, String roomId, int seatId) { }
    private final Map<Key, ConnectionPresence> values = new ConcurrentHashMap<>();
    private final Clock clock;
    public ConnectionPresenceRegistry() { this(Clock.systemUTC()); }
    public ConnectionPresenceRegistry(Clock clock) { this.clock = java.util.Objects.requireNonNull(clock); }
    public ConnectionPresence connected(ConnectionSession session) {
        Key key = key(session); Instant now = clock.instant();
        return values.compute(key, (ignored, current) -> {
            if (current != null && session.generation() < current.generation()) return current;
            return presence(session, ConnectionPresence.Status.ONLINE, now);
        });
    }
    public boolean disconnected(ConnectionSession session) {
        Key key = key(session); Instant now = clock.instant(); boolean[] changed = { false };
        values.computeIfPresent(key, (ignored, current) -> {
            if (current.generation() != session.generation() || !current.connectionId().equals(session.connectionId())) return current;
            changed[0] = true; return presence(session, ConnectionPresence.Status.OFFLINE, now);
        });
        return changed[0];
    }
    public Optional<ConnectionPresence> find(String userId, String roomId, int seatId) { return Optional.ofNullable(values.get(new Key(userId, roomId, seatId))); }
    private static Key key(ConnectionSession session) { return new Key(session.userId(), session.roomId(), session.seatId()); }
    private static ConnectionPresence presence(ConnectionSession session, ConnectionPresence.Status status, Instant now) {
        return new ConnectionPresence(session.userId(), session.roomId(), session.seatId(), status,
                session.connectionId(), session.generation(), now);
    }
}
