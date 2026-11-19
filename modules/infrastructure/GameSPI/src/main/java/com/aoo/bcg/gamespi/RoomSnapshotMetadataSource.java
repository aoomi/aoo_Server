package com.aoo.bcg.gamespi;

import java.util.Objects;

/**
 * Neutral, immutable source projection for canonical room snapshot metadata.
 *
 * <p>This contract is deliberately separate from
 * {@link RoomCreationContext.CompatibilityOrigin}. It carries exactly the ten
 * canonical metadata fields and does not infer values from a game id, room
 * type, default version, clock, or historical Compatibility state.</p>
 */
public record RoomSnapshotMetadataSource(
        Long releaseId,
        String ruleContentHash,
        String componentContentHash,
        String cardCodecVersion,
        String eventInterpreterVersion,
        String protocolVersion,
        String snapshotSchemaVersion,
        Long indexGeneration,
        String regionCode,
        String sourceHash) {

    private static final String SHA256 = "[0-9a-fA-F]{64}";

    public RoomSnapshotMetadataSource {
        if (releaseId == null || releaseId <= 0) {
            throw new IllegalArgumentException("ROOM_SNAPSHOT_METADATA_RELEASE_REQUIRED");
        }
        requireSha256(ruleContentHash, "ROOM_SNAPSHOT_METADATA_RULE_CONTENT_HASH_INVALID");
        requireSha256(componentContentHash, "ROOM_SNAPSHOT_METADATA_COMPONENT_CONTENT_HASH_INVALID");
        requireText(cardCodecVersion, "ROOM_SNAPSHOT_METADATA_CARD_CODEC_REQUIRED");
        requireText(eventInterpreterVersion, "ROOM_SNAPSHOT_METADATA_EVENT_INTERPRETER_REQUIRED");
        requireText(protocolVersion, "ROOM_SNAPSHOT_METADATA_PROTOCOL_REQUIRED");
        requireText(snapshotSchemaVersion, "ROOM_SNAPSHOT_METADATA_SCHEMA_REQUIRED");
        if (indexGeneration == null || indexGeneration <= 0) {
            throw new IllegalArgumentException("ROOM_SNAPSHOT_METADATA_INDEX_GENERATION_REQUIRED");
        }
        requireText(regionCode, "ROOM_SNAPSHOT_METADATA_REGION_REQUIRED");
        requireSha256(sourceHash, "ROOM_SNAPSHOT_METADATA_SOURCE_HASH_INVALID");
    }

    private static void requireText(String value, String errorCode) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(errorCode);
    }

    private static void requireSha256(String value, String errorCode) {
        requireText(value, errorCode);
        if (!value.matches(SHA256)) throw new IllegalArgumentException(errorCode);
    }
}
