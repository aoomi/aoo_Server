package com.aoo.bcg.gamespi;
import java.util.Optional;

public record GameRoomHandle(long roomId, int gameId, String playVersion, Object legacyRoom) {
    public GameRoomHandle {
        if (roomId <= 0 || gameId <= 0 || playVersion == null || playVersion.isBlank() || legacyRoom == null) throw new IllegalArgumentException("invalid game room handle");
    }
    public <T> T requireLegacyRoom(Class<T> roomType) {
        Object value = legacyRoom instanceof LegacyCompatibleRoom bridge ? bridge.legacyRoom() : legacyRoom;
        return roomType.cast(value);
    }
    public AuthoritativeGameSession requireAuthoritativeSession() {
        if (legacyRoom instanceof AuthoritativeGameSession session) return session;
        if (legacyRoom instanceof LegacyCompatibleRoom bridge) return bridge.authoritativeSession();
        throw new IllegalStateException("room has no unified authoritative session");
    }
    public Optional<AuthoritativeGameSession> authoritativeSession() {
        if (legacyRoom instanceof AuthoritativeGameSession session) return Optional.of(session);
        if (legacyRoom instanceof LegacyCompatibleRoom bridge) return Optional.of(bridge.authoritativeSession());
        return Optional.empty();
    }
}
