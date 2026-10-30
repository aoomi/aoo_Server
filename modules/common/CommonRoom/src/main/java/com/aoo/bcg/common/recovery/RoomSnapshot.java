package com.aoo.bcg.common.recovery;

import java.time.Instant;
import java.util.Map;
import com.aoo.bcg.gamespi.StatePayload;

public record RoomSnapshot(long roomId, int gameId, String playVersion, String componentVersion,
                           long fencingToken, long stateVersion,long lastEventSequence, Instant capturedAt,
                           StatePayload authoritativeState, RoomSnapshotMetadata metadata) {
    public RoomSnapshot {
        if (roomId <= 0 || stateVersion < 0 || lastEventSequence < 0
                || stateVersion != lastEventSequence) {
            throw new IllegalArgumentException("snapshot stateVersion must equal committed event sequence");
        }
        authoritativeState = authoritativeState == null ? StatePayload.empty() : authoritativeState;
        metadata = metadata == null ? RoomSnapshotMetadata.missing() : metadata;
    }

    public RoomSnapshot(long roomId, int gameId, String playVersion, String componentVersion,
                        long fencingToken, long stateVersion, long lastEventSequence,
                        Instant capturedAt, StatePayload authoritativeState) {
        this(roomId, gameId, playVersion, componentVersion, fencingToken, stateVersion,
                lastEventSequence, capturedAt, authoritativeState, RoomSnapshotMetadata.missing());
    }

    public RoomSnapshot(long roomId, int gameId, String playVersion, String componentVersion,
                        long fencingToken, long stateVersion, long lastEventSequence,
                        Instant capturedAt, Map<String, Object> authoritativeState) {
        this(roomId, gameId, playVersion, componentVersion, fencingToken, stateVersion,
                lastEventSequence, capturedAt, StatePayload.copyOf(authoritativeState),
                RoomSnapshotMetadata.missing());
    }

    public RoomSnapshot(long roomId, int gameId, String playVersion, String componentVersion,
                        long fencingToken, long lastEventSequence, Instant capturedAt,
                        Map<String, Object> authoritativeState) {
        this(roomId, gameId, playVersion, componentVersion, fencingToken, lastEventSequence,
                lastEventSequence, capturedAt, StatePayload.copyOf(authoritativeState),
                RoomSnapshotMetadata.missing());
    }

    public RoomSnapshot(long roomId, int gameId, String playVersion, String componentVersion,
                        long fencingToken, long stateVersion, long lastEventSequence,
                        Instant capturedAt, Map<String, Object> authoritativeState,
                        RoomSnapshotMetadata metadata) {
        this(roomId, gameId, playVersion, componentVersion, fencingToken, stateVersion,
                lastEventSequence, capturedAt, StatePayload.copyOf(authoritativeState), metadata);
    }
}
