package com.aoo.bcg.gamespi;

public record BridgedGameRoom(Object legacyRoom, AuthoritativeGameSession authoritativeSession)
        implements LegacyCompatibleRoom {
    public BridgedGameRoom {
        if (legacyRoom == null || authoritativeSession == null) throw new IllegalArgumentException("bridge values required");
    }
}
