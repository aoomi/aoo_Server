package com.aoo.bcg.gateway;
public final class InMemoryConnectionGenerationStore implements ConnectionGenerationStore {
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.atomic.AtomicLong> values = new java.util.concurrent.ConcurrentHashMap<>();
    @Override public long next(String userId, String roomId, int seatId) {
        if (userId == null || userId.isBlank() || roomId == null || roomId.isBlank() || seatId < 0) throw new IllegalArgumentException("invalid connection scope");
        return values.computeIfAbsent(userId + '\n' + roomId + '\n' + seatId, ignored -> new java.util.concurrent.atomic.AtomicLong()).incrementAndGet();
    }
}
