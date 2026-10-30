package com.aoo.bcg.common.recovery;

/**
 * Explicit read-side projection of the rule/release lock for one room snapshot.
 *
 * <p>This is an input contract only. It does not read a database, derive values from a room, or
 * provide defaults. The caller must supply the complete canonical metadata source and the static
 * game identity that was locked when the snapshot was produced.</p>
 */
public record RoomSnapshotRuleLockSource(
        long gameId,
        String playVersion,
        CanonicalRoomSnapshotMetadataSource metadataSource) {

    public RoomSnapshotRuleLockSource {
        if (gameId <= 0) {
            throw new IllegalArgumentException("ROOM_RULE_LOCK_GAME_ID_REQUIRED");
        }
        requireText(playVersion, "playVersion");
        if (metadataSource == null) {
            throw new IllegalArgumentException("ROOM_RULE_LOCK_METADATA_SOURCE_REQUIRED");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("ROOM_RULE_LOCK_" + field.toUpperCase() + "_REQUIRED");
        }
    }
}
