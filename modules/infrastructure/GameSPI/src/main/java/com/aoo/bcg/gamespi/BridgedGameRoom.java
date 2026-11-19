package com.aoo.bcg.gamespi;

public record BridgedGameRoom(Object legacyRoom, AuthoritativeGameSession authoritativeSession,
                              RoomCreationContext.CompatibilityOrigin compatibilityOrigin,
                              RoomSnapshotMetadataSource snapshotMetadataSource)
        implements LegacyCompatibleRoom {
    public BridgedGameRoom(Object legacyRoom, AuthoritativeGameSession authoritativeSession) {
        this(legacyRoom, authoritativeSession, null, null);
    }

    public BridgedGameRoom(Object legacyRoom, AuthoritativeGameSession authoritativeSession,
                           RoomCreationContext.CompatibilityOrigin compatibilityOrigin) {
        this(legacyRoom, authoritativeSession, compatibilityOrigin, null);
    }

    public BridgedGameRoom {
        if (legacyRoom == null || authoritativeSession == null) throw new IllegalArgumentException("bridge values required");
        if (compatibilityOrigin != null) compatibilityOrigin.requireSnapshotMetadataSource();
    }

    public RoomSnapshotMetadataSource requireRoomSnapshotMetadataSource() {
        if (snapshotMetadataSource == null) {
            throw new IllegalStateException("ROOM_SNAPSHOT_METADATA_SOURCE_REQUIRED");
        }
        return snapshotMetadataSource;
    }
}
