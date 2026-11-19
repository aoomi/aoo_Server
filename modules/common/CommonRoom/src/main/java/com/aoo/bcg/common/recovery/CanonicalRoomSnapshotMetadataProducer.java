package com.aoo.bcg.common.recovery;

/**
 * The single CommonRoom construction boundary for complete room snapshot metadata.
 *
 * <p>The producer has no defaults and no ambient inputs. A later committer may pass one explicit
 * source projection, or pass a second independently obtained projection for exact consistency
 * checking. Any missing, malformed, or conflicting source is rejected before a metadata value is
 * created.</p>
 */
public final class CanonicalRoomSnapshotMetadataProducer {
    private static final String HASH = "[0-9a-fA-F]{64}";

    /** Build metadata from one complete canonical source projection. */
    public RoomSnapshotMetadata produce(CanonicalRoomSnapshotMetadataSource source) {
        validate(source);
        return new RoomSnapshotMetadata(
                source.releaseId(),
                source.ruleContentHash(),
                source.componentContentHash(),
                source.cardCodecVersion(),
                source.eventInterpreterVersion(),
                source.protocolVersion(),
                source.snapshotSchemaVersion(),
                source.indexGeneration(),
                source.regionCode(),
                source.sourceHash());
    }

    /**
     * Build metadata only when two explicit source projections are exactly identical.
     *
     * <p>This overload is for an assembly point that receives a canonical source and a
     * corroborating projection. It prevents a committer from silently choosing one of two
     * conflicting release/rule/component/version sources.</p>
     */
    public RoomSnapshotMetadata produce(CanonicalRoomSnapshotMetadataSource source,
                                        CanonicalRoomSnapshotMetadataSource corroboratingSource) {
        validate(source);
        validate(corroboratingSource);
        if (!source.equals(corroboratingSource)) {
            throw new IllegalStateException("ROOM_SNAPSHOT_METADATA_SOURCE_CONFLICT");
        }
        return produce(source);
    }

    private static void validate(CanonicalRoomSnapshotMetadataSource source) {
        if (source == null) {
            throw new IllegalStateException("ROOM_SNAPSHOT_METADATA_SOURCE_REQUIRED");
        }
        if (source.releaseId() == null || source.releaseId() <= 0) {
            throw new IllegalStateException("ROOM_SNAPSHOT_METADATA_RELEASE_REQUIRED");
        }
        if (source.indexGeneration() == null || source.indexGeneration() <= 0) {
            throw new IllegalStateException("ROOM_SNAPSHOT_METADATA_INDEX_GENERATION_REQUIRED");
        }
        requireHash(source.ruleContentHash(), "ruleContentHash");
        requireHash(source.componentContentHash(), "componentContentHash");
        requireHash(source.sourceHash(), "sourceHash");
        requireText(source.cardCodecVersion(), "cardCodecVersion");
        requireText(source.eventInterpreterVersion(), "eventInterpreterVersion");
        requireText(source.protocolVersion(), "protocolVersion");
        requireText(source.snapshotSchemaVersion(), "snapshotSchemaVersion");
        requireText(source.regionCode(), "regionCode");
    }

    private static void requireHash(String value, String field) {
        if (value == null || !value.matches(HASH)) {
            throw new IllegalStateException("ROOM_SNAPSHOT_METADATA_" + field.toUpperCase() + "_INVALID");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("ROOM_SNAPSHOT_METADATA_" + field.toUpperCase() + "_REQUIRED");
        }
    }
}
