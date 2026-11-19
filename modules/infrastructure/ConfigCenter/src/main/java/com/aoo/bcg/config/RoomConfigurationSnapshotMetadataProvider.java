package com.aoo.bcg.config;

import com.aoo.bcg.gamespi.RoomSnapshotMetadataSource;

import java.util.Optional;

/**
 * Neutral adapter over the canonical room-lock reader.
 *
 * <p>It exposes only the complete source already persisted in one room-lock
 * row; missing or malformed rows fail through the LeaseStore and no value is
 * inferred from a game descriptor, release, bundle or UI hash.  The metadata
 * path deliberately does not construct the legacy RoomRuleSnapshot, whose
 * scoreVersion is outside this canonical metadata contract.</p>
 */
public final class RoomConfigurationSnapshotMetadataProvider {
    private final RoomConfigurationLeaseStore leases;

    public RoomConfigurationSnapshotMetadataProvider(RoomConfigurationLeaseStore leases) {
        this.leases = java.util.Objects.requireNonNull(leases, "leases");
    }

    public Optional<RoomSnapshotMetadataSource> find(long roomId) {
        return leases.findMetadata(roomId).map(lease -> lease.metadata().neutralSource());
    }

    public RoomSnapshotMetadataSource require(long roomId) {
        return leases.requireMetadata(roomId).metadata().neutralSource();
    }

    /**
     * Returns the identity and neutral metadata from the same locked row.
     * Consumers must validate this projection against their request; they may
     * not replace a missing field with a route, catalog, or release value.
     */
    public CanonicalProjection requireProjection(long roomId) {
        var lease = leases.requireMetadata(roomId);
        var metadata = lease.metadata();
        return new CanonicalProjection(metadata.gameId(), metadata.playVersion(), metadata.regionCode(),
                metadata.indexGeneration(), metadata.neutralSource());
    }

    public record CanonicalProjection(int gameId, String playVersion, String regionCode,
                                      long indexGeneration, RoomSnapshotMetadataSource source) {
        public CanonicalProjection {
            if (gameId <= 0 || indexGeneration <= 0 || source == null
                    || regionCode == null || regionCode.isBlank()
                    || playVersion == null || playVersion.isBlank()) {
                throw new IllegalArgumentException("ROOM_CONFIGURATION_LOCK_PROJECTION_INVALID");
            }
            if (!source.regionCode().equals(regionCode) || source.indexGeneration() != indexGeneration) {
                throw new IllegalArgumentException("ROOM_CONFIGURATION_LOCK_PROJECTION_CONFLICT");
            }
        }
    }
}
