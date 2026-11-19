package com.aoo.bcg.gamespi;
import java.util.Optional;

public record GameRoomHandle(long roomId, int gameId, String playVersion, Object legacyRoom,
                             RoomCreationContext.CompatibilityOrigin compatibilityOrigin,
                             RoomSnapshotMetadataSource snapshotMetadataSource,
                             RoomRuntimeIdentity runtimeIdentity) {
    public GameRoomHandle(long roomId, int gameId, String playVersion, Object legacyRoom) {
        this(roomId, gameId, playVersion, legacyRoom, null, null, null);
    }

    public GameRoomHandle(long roomId, int gameId, String playVersion, Object legacyRoom,
                          RoomCreationContext.CompatibilityOrigin compatibilityOrigin) {
        this(roomId, gameId, playVersion, legacyRoom, compatibilityOrigin, null, null);
    }

    public GameRoomHandle(long roomId, int gameId, String playVersion, Object legacyRoom,
                          RoomCreationContext.CompatibilityOrigin compatibilityOrigin,
                          RoomSnapshotMetadataSource snapshotMetadataSource) {
        this(roomId, gameId, playVersion, legacyRoom, compatibilityOrigin,
                snapshotMetadataSource, null);
    }

    public GameRoomHandle(long roomId, int gameId, String playVersion, Object legacyRoom,
                          RoomRuntimeIdentity runtimeIdentity) {
        this(roomId, gameId, playVersion, legacyRoom, null, null, runtimeIdentity);
    }

    public GameRoomHandle {
        if (roomId <= 0 || gameId <= 0 || playVersion == null || playVersion.isBlank() || legacyRoom == null) throw new IllegalArgumentException("invalid game room handle");
        if (compatibilityOrigin != null
                && (compatibilityOrigin.staticIdentity().gameId() != gameId
                || !compatibilityOrigin.staticIdentity().version().equals(playVersion))) {
            throw new IllegalArgumentException("COMPATIBILITY_ORIGIN_HANDLE_IDENTITY_MISMATCH");
        }
        if (compatibilityOrigin != null) compatibilityOrigin.requireSnapshotMetadataSource();
        if (runtimeIdentity != null && runtimeIdentity.stateVersion() < 0) {
            throw new IllegalArgumentException("ROOM_RUNTIME_STATE_VERSION_INVALID");
        }
    }

    public RoomCreationContext.CompatibilityOrigin requireCompatibilityOrigin() {
        if (compatibilityOrigin == null) throw new IllegalStateException("COMPATIBILITY_ORIGIN_REQUIRED");
        return compatibilityOrigin;
    }

    public RoomSnapshotMetadataSource requireRoomSnapshotMetadataSource() {
        if (snapshotMetadataSource == null) {
            throw new IllegalStateException("ROOM_SNAPSHOT_METADATA_SOURCE_REQUIRED");
        }
        return snapshotMetadataSource;
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
