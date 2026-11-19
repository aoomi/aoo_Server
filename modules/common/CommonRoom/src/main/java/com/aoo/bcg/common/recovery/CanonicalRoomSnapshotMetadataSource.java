package com.aoo.bcg.common.recovery;

/**
 * Explicit, immutable source projection for canonical room snapshot metadata.
 *
 * <p>This type is deliberately a value carrier. Validation and construction of durable metadata
 * belong to {@link CanonicalRoomSnapshotMetadataProducer}; callers must provide every field and
 * must not derive a value from a game id, room type, default version, or wall clock.</p>
 */
public record CanonicalRoomSnapshotMetadataSource(
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
}
