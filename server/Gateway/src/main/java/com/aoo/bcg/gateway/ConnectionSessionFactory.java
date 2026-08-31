package com.aoo.bcg.gateway;

import java.util.UUID;
import java.util.function.Supplier;

public final class ConnectionSessionFactory {
    private final ConnectionGenerationStore generations;
    private final Supplier<String> connectionIds;
    public ConnectionSessionFactory(ConnectionGenerationStore generations) { this(generations, () -> UUID.randomUUID().toString()); }
    ConnectionSessionFactory(ConnectionGenerationStore generations, Supplier<String> connectionIds) {
        this.generations = java.util.Objects.requireNonNull(generations); this.connectionIds = java.util.Objects.requireNonNull(connectionIds);
    }
    public ConnectionSession open(String userId, String roomId, int seatId, String playVersion, long lastSequence) {
        long generation = generations.next(userId, roomId, seatId);
        return new ConnectionSession(userId, roomId, seatId, playVersion, lastSequence, connectionIds.get(), generation);
    }
}
