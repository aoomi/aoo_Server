package com.aoo.bcg.common.recovery;

import java.util.Objects;
import java.util.Set;

/**
 * Pure fail-closed read gate for a previously materialized {@link RoomSnapshot}.
 *
 * <p>The gate only compares explicit inputs. It does not load recovery state, contact a database,
 * mutate a room, or infer a missing release/rule/version value. Integration with recovery,
 * reconnect, readiness, and committers is intentionally outside this contract.</p>
 */
public final class RoomSnapshotReadGate {
    public static final String SUPPORTED_SNAPSHOT_SCHEMA_VERSION = "room-snapshot-v1";
    public static final String SUPPORTED_CARD_CODEC_VERSION = "card-v3";
    public static final String SUPPORTED_EVENT_INTERPRETER_VERSION = "events-v4";
    public static final String SUPPORTED_PROTOCOL_VERSION = "protocol-v2";

    private static final Set<String> SUPPORTED_CARD_CODEC_VERSIONS =
            Set.of(SUPPORTED_CARD_CODEC_VERSION);
    private static final Set<String> SUPPORTED_EVENT_INTERPRETER_VERSIONS =
            Set.of(SUPPORTED_EVENT_INTERPRETER_VERSION);
    private static final Set<String> SUPPORTED_PROTOCOL_VERSIONS =
            Set.of(SUPPORTED_PROTOCOL_VERSION);

    private RoomSnapshotReadGate() {
    }

    /**
     * Validate a snapshot against the explicit rule/release lock and return the same immutable
     * values only when every identity, sidecar, and supported-version check succeeds.
     */
    public static VerifiedSnapshot requireReadable(RoomSnapshot snapshot,
                                                    RoomSnapshotRuleLockSource ruleLock) {
        if (snapshot == null) {
            throw reject(Failure.SNAPSHOT_REQUIRED);
        }
        if (ruleLock == null) {
            throw reject(Failure.RULE_LOCK_REQUIRED);
        }

        RoomSnapshotMetadata snapshotMetadata = snapshot.metadata();
        if (snapshotMetadata == null || !snapshotMetadata.isComplete()) {
            throw reject(Failure.SNAPSHOT_METADATA_REQUIRED);
        }

        requireSupportedVersions(snapshotMetadata);

        RoomSnapshotMetadata lockedMetadata;
        try {
            lockedMetadata = new CanonicalRoomSnapshotMetadataProducer()
                    .produce(ruleLock.metadataSource());
        } catch (RuntimeException invalidSource) {
            throw reject(Failure.RULE_LOCK_SOURCE_INVALID);
        }
        requireSupportedVersions(lockedMetadata);

        if (snapshot.gameId() != ruleLock.gameId()
                || !Objects.equals(snapshot.playVersion(), ruleLock.playVersion())
                || !snapshotMetadata.equals(lockedMetadata)) {
            throw reject(Failure.RULE_LOCK_CONFLICT);
        }

        return new VerifiedSnapshot(snapshot, ruleLock);
    }

    private static void requireSupportedVersions(RoomSnapshotMetadata metadata) {
        if (!SUPPORTED_SNAPSHOT_SCHEMA_VERSION.equals(metadata.snapshotSchemaVersion())) {
            throw reject(Failure.SNAPSHOT_SCHEMA_UNSUPPORTED);
        }
        if (!SUPPORTED_CARD_CODEC_VERSIONS.contains(metadata.cardCodecVersion())
                || !SUPPORTED_EVENT_INTERPRETER_VERSIONS.contains(metadata.eventInterpreterVersion())
                || !SUPPORTED_PROTOCOL_VERSIONS.contains(metadata.protocolVersion())) {
            throw reject(Failure.STATIC_VERSION_UNSUPPORTED);
        }
    }

    private static RejectedSnapshotException reject(Failure failure) {
        return new RejectedSnapshotException(failure);
    }

    public enum Failure {
        SNAPSHOT_REQUIRED,
        RULE_LOCK_REQUIRED,
        SNAPSHOT_METADATA_REQUIRED,
        SNAPSHOT_SCHEMA_UNSUPPORTED,
        STATIC_VERSION_UNSUPPORTED,
        RULE_LOCK_SOURCE_INVALID,
        RULE_LOCK_CONFLICT
    }

    public record VerifiedSnapshot(RoomSnapshot snapshot, RoomSnapshotRuleLockSource ruleLock) {
        public VerifiedSnapshot {
            if (snapshot == null || ruleLock == null) {
                throw new IllegalArgumentException("ROOM_SNAPSHOT_READ_GATE_RESULT_REQUIRED");
            }
        }
    }

    public static final class RejectedSnapshotException extends IllegalStateException {
        private final Failure failure;

        private RejectedSnapshotException(Failure failure) {
            super(failure.name());
            this.failure = failure;
        }

        public Failure failure() {
            return failure;
        }
    }
}
