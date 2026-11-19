package com.aoo.bcg.gamespi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Neutral provider contract for an already-authorized canonical snapshot
 * source. It is a passive GameSPI boundary; it does not read JDBC, discover
 * providers, or derive metadata from a room/game id.
 */
@FunctionalInterface
public interface RoomSnapshotMetadataSourceProvider {
    Resolution resolve(long roomId, GameDescriptor staticIdentity);

    /**
     * Resolve one source and prove that the provider returned it for the same
     * static identity requested by the caller.
     */
    default RoomSnapshotMetadataSource requireSource(long roomId, GameDescriptor staticIdentity) {
        if (roomId <= 0) throw new IllegalArgumentException("ROOM_SNAPSHOT_METADATA_ROOM_REQUIRED");
        Objects.requireNonNull(staticIdentity, "staticIdentity");
        Resolution resolution = resolve(roomId, staticIdentity);
        if (resolution == null) {
            throw new IllegalStateException("ROOM_SNAPSHOT_METADATA_SOURCE_REQUIRED");
        }
        if (!staticIdentity.equals(resolution.staticIdentity())) {
            throw new IllegalStateException("ROOM_SNAPSHOT_METADATA_SOURCE_IDENTITY_CONFLICT");
        }
        if (resolution.source() == null) {
            throw new IllegalStateException("ROOM_SNAPSHOT_METADATA_SOURCE_REQUIRED");
        }
        return resolution.source();
    }

    /**
     * Select exactly one provider. Missing and duplicate providers are both
     * rejected so production assembly cannot silently choose a source.
     */
    static RoomSnapshotMetadataSourceProvider requireSingle(
            Iterable<? extends RoomSnapshotMetadataSourceProvider> providers) {
        Objects.requireNonNull(providers, "providers");
        List<RoomSnapshotMetadataSourceProvider> candidates = new ArrayList<>();
        for (RoomSnapshotMetadataSourceProvider provider : providers) {
            if (provider == null) throw new IllegalStateException("ROOM_SNAPSHOT_METADATA_PROVIDER_NULL");
            candidates.add(provider);
        }
        if (candidates.isEmpty()) {
            throw new IllegalStateException("ROOM_SNAPSHOT_METADATA_PROVIDER_REQUIRED");
        }
        if (candidates.size() != 1) {
            throw new IllegalStateException("ROOM_SNAPSHOT_METADATA_PROVIDER_CONFLICT");
        }
        return candidates.get(0);
    }

    /** Immutable identity/source pair returned by an authorized provider. */
    record Resolution(GameDescriptor staticIdentity, RoomSnapshotMetadataSource source) {
        public Resolution {
            Objects.requireNonNull(staticIdentity, "staticIdentity");
            Objects.requireNonNull(source, "source");
        }
    }
}
