package com.aoo.bcg.gamespi;

/**
 * Explicit runtime identity used to fence a room instance from a later
 * lifecycle generation. Values are supplied by the authority; this type never
 * derives or allocates them.
 */
public record RoomRuntimeIdentity(String roomInstanceId, long lifecycleEpoch,
                                  long fencingToken, long stateVersion) {
    public RoomRuntimeIdentity {
        if (roomInstanceId == null || roomInstanceId.isBlank()) {
            throw new IllegalArgumentException("ROOM_RUNTIME_INSTANCE_ID_REQUIRED");
        }
        if (lifecycleEpoch <= 0) {
            throw new IllegalArgumentException("ROOM_RUNTIME_LIFECYCLE_EPOCH_REQUIRED");
        }
        if (fencingToken <= 0) {
            throw new IllegalArgumentException("ROOM_RUNTIME_FENCING_TOKEN_REQUIRED");
        }
        if (stateVersion < 0) {
            throw new IllegalArgumentException("ROOM_RUNTIME_STATE_VERSION_INVALID");
        }
    }

    public boolean sameGeneration(RoomRuntimeIdentity other) {
        return other != null
                && roomInstanceId.equals(other.roomInstanceId)
                && lifecycleEpoch == other.lifecycleEpoch
                && fencingToken == other.fencingToken;
    }
}
