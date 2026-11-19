package com.aoo.bcg.common.recovery;

/**
 * Immutable release proof carried by every authoritative room snapshot.
 *
 * <p>{@link #missing()} is an explicit pre-migration marker only. Persistence adapters must call
 * {@link #requireComplete()} before writing it; no adapter may replace missing values with a
 * gameplay default.</p>
 */
public record RoomSnapshotMetadata(
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

    private static final String HASH = "[0-9a-fA-F]{64}";

    public RoomSnapshotMetadata {
        if (releaseId != null && releaseId <= 0) throw new IllegalArgumentException("releaseId must be positive");
        if (indexGeneration != null && indexGeneration <= 0) {
            throw new IllegalArgumentException("indexGeneration must be positive");
        }
        validateHash(ruleContentHash, "ruleContentHash");
        validateHash(componentContentHash, "componentContentHash");
        validateHash(sourceHash, "sourceHash");
        validateText(cardCodecVersion, "cardCodecVersion");
        validateText(eventInterpreterVersion, "eventInterpreterVersion");
        validateText(protocolVersion, "protocolVersion");
        validateText(snapshotSchemaVersion, "snapshotSchemaVersion");
        validateText(regionCode, "regionCode");
    }

    /** Explicit missing metadata marker; it is never valid for a durable write. */
    public static RoomSnapshotMetadata missing() {
        return new RoomSnapshotMetadata(null, null, null, null, null, null, null, null, null, null);
    }

    public boolean isComplete() {
        return releaseId != null && indexGeneration != null
                && ruleContentHash != null && componentContentHash != null && sourceHash != null
                && cardCodecVersion != null && eventInterpreterVersion != null
                && protocolVersion != null && snapshotSchemaVersion != null && regionCode != null;
    }

    public void requireComplete() {
        if (!isComplete()) throw new IllegalStateException("ROOM_SNAPSHOT_METADATA_REQUIRED");
    }

    /**
     * Returns the exact sidecar projection supported by the current snapshot table.
     *
     * <p>The remaining contract fields are deliberately not projected here because the current
     * database schema does not contain columns for them.  A missing canonical producer therefore
     * still fails at this boundary; callers must not fill the projection with defaults.</p>
     */
    public SnapshotColumns requireSnapshotColumns() {
        requireComplete();
        return new SnapshotColumns(releaseId, ruleContentHash, componentContentHash,
                cardCodecVersion, eventInterpreterVersion, protocolVersion);
    }

    /** The six sidecar columns currently present on {@code aoo_room_snapshot}. */
    public record SnapshotColumns(Long releaseId, String ruleContentHash,
                                  String componentContentHash, String cardCodecVersion,
                                  String eventInterpreterVersion, String protocolVersion) {
        public SnapshotColumns {
            if (releaseId == null || releaseId <= 0) throw new IllegalArgumentException("releaseId must be positive");
            validateHash(ruleContentHash, "ruleContentHash");
            validateHash(componentContentHash, "componentContentHash");
            validateText(cardCodecVersion, "cardCodecVersion");
            validateText(eventInterpreterVersion, "eventInterpreterVersion");
            validateText(protocolVersion, "protocolVersion");
        }
    }

    private static void validateHash(String value, String field) {
        if (value != null && !value.matches(HASH)) {
            throw new IllegalArgumentException(field + " must be a 64-character hex hash");
        }
    }

    private static void validateText(String value, String field) {
        if (value != null && value.isBlank()) throw new IllegalArgumentException(field + " must not be blank");
    }
}
