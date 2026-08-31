package com.aoo.bcg.gamespi;

import java.util.Map;

public record RoomCreationContext(long roomId, long ownerId, RulePayload immutableRules) {
    public RoomCreationContext {
        if (roomId <= 0 || ownerId <= 0) throw new IllegalArgumentException("roomId and ownerId must be positive");
        immutableRules = immutableRules == null ? RulePayload.empty() : immutableRules;
    }
    public RoomCreationContext(long roomId, long ownerId, Map<String, ?> immutableRules) {
        this(roomId, ownerId, RulePayload.copyOf(immutableRules));
    }
}
