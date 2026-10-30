package com.aoo.bcg.common.recovery;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomSnapshotReadGateTest {
    private static final String HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void acceptsCompleteSnapshotOnlyWhenStaticAndSidecarIdentityMatches() {
        RoomSnapshot snapshot = snapshot(metadata("room-snapshot-v1", "card-v3", "events-v4", "protocol-v2"));

        RoomSnapshotReadGate.VerifiedSnapshot verified = RoomSnapshotReadGate.requireReadable(snapshot, ruleLock());

        assertEquals(snapshot, verified.snapshot());
        assertEquals(ruleLock(), verified.ruleLock());
    }

    @Test
    void rejectsMissingSnapshotAndRuleLock() {
        RoomSnapshotReadGate.RejectedSnapshotException missingSnapshot = assertThrows(
                RoomSnapshotReadGate.RejectedSnapshotException.class,
                () -> RoomSnapshotReadGate.requireReadable(null, ruleLock()));
        assertEquals(RoomSnapshotReadGate.Failure.SNAPSHOT_REQUIRED, missingSnapshot.failure());
        assertEquals(RoomSnapshotReadGate.Failure.SNAPSHOT_REQUIRED.name(), missingSnapshot.getMessage());

        RoomSnapshotReadGate.RejectedSnapshotException missingRuleLock = assertThrows(
                RoomSnapshotReadGate.RejectedSnapshotException.class,
                () -> RoomSnapshotReadGate.requireReadable(snapshot(completeMetadata()), null));
        assertEquals(RoomSnapshotReadGate.Failure.RULE_LOCK_REQUIRED, missingRuleLock.failure());
        assertEquals(RoomSnapshotReadGate.Failure.RULE_LOCK_REQUIRED.name(), missingRuleLock.getMessage());
    }

    @Test
    void rejectsMissingSidecar() {
        RoomSnapshotReadGate.RejectedSnapshotException failure = assertThrows(
                RoomSnapshotReadGate.RejectedSnapshotException.class,
                () -> RoomSnapshotReadGate.requireReadable(snapshot(RoomSnapshotMetadata.missing()), ruleLock()));
        assertEquals(RoomSnapshotReadGate.Failure.SNAPSHOT_METADATA_REQUIRED, failure.failure());
        assertEquals(RoomSnapshotReadGate.Failure.SNAPSHOT_METADATA_REQUIRED.name(), failure.getMessage());
    }

    @Test
    void rejectsMissingRuleLockMetadataSource() {
        RoomSnapshotReadGate.RejectedSnapshotException failure = assertThrows(
                RoomSnapshotReadGate.RejectedSnapshotException.class,
                () -> RoomSnapshotReadGate.requireReadable(snapshot(completeMetadata()),
                        new RoomSnapshotRuleLockSource(1001, "chengdu1001-v1", incompleteSource())));
        assertEquals(RoomSnapshotReadGate.Failure.RULE_LOCK_SOURCE_INVALID, failure.failure());
        assertEquals(RoomSnapshotReadGate.Failure.RULE_LOCK_SOURCE_INVALID.name(), failure.getMessage());
    }

    @Test
    void rejectsConflictingRuleLockField() {
        CanonicalRoomSnapshotMetadataSource conflict = new CanonicalRoomSnapshotMetadataSource(
                77L, HASH, HASH, "card-v3", "events-v4", "protocol-v2",
                "room-snapshot-v1", 13L, "CN-SC", HASH);

        RoomSnapshotReadGate.RejectedSnapshotException failure = assertThrows(
                RoomSnapshotReadGate.RejectedSnapshotException.class,
                () -> RoomSnapshotReadGate.requireReadable(snapshot(completeMetadata()),
                        new RoomSnapshotRuleLockSource(1001, "chengdu1001-v1", conflict)));
        assertEquals(RoomSnapshotReadGate.Failure.RULE_LOCK_CONFLICT, failure.failure());
        assertEquals(RoomSnapshotReadGate.Failure.RULE_LOCK_CONFLICT.name(), failure.getMessage());
    }

    @Test
    void rejectsNullSnapshotPlayVersionWithoutThrowingNullPointerException() {
        RoomSnapshotReadGate.RejectedSnapshotException failure = assertThrows(
                RoomSnapshotReadGate.RejectedSnapshotException.class,
                () -> RoomSnapshotReadGate.requireReadable(snapshotWithPlayVersion(null), ruleLock()));
        assertEquals(RoomSnapshotReadGate.Failure.RULE_LOCK_CONFLICT, failure.failure());
        assertEquals(RoomSnapshotReadGate.Failure.RULE_LOCK_CONFLICT.name(), failure.getMessage());
    }

    @Test
    void rejectsMalformedSnapshotPlayVersionWithoutThrowingNullPointerException() {
        RoomSnapshotReadGate.RejectedSnapshotException failure = assertThrows(
                RoomSnapshotReadGate.RejectedSnapshotException.class,
                () -> RoomSnapshotReadGate.requireReadable(snapshotWithPlayVersion(" "), ruleLock()));
        assertEquals(RoomSnapshotReadGate.Failure.RULE_LOCK_CONFLICT, failure.failure());
        assertEquals(RoomSnapshotReadGate.Failure.RULE_LOCK_CONFLICT.name(), failure.getMessage());
    }

    @Test
    void rejectsUnknownStaticVersion() {
        RoomSnapshotReadGate.RejectedSnapshotException failure = assertThrows(
                RoomSnapshotReadGate.RejectedSnapshotException.class,
                () -> RoomSnapshotReadGate.requireReadable(
                        snapshot(metadata("room-snapshot-v1", "card-v99", "events-v4", "protocol-v2")),
                        ruleLockWith("card-v99", "events-v4", "protocol-v2")));
        assertEquals(RoomSnapshotReadGate.Failure.STATIC_VERSION_UNSUPPORTED, failure.failure());
        assertEquals(RoomSnapshotReadGate.Failure.STATIC_VERSION_UNSUPPORTED.name(), failure.getMessage());
    }

    @Test
    void rejectsUnknownVersionInRuleLockSource() {
        RoomSnapshotReadGate.RejectedSnapshotException failure = assertThrows(
                RoomSnapshotReadGate.RejectedSnapshotException.class,
                () -> RoomSnapshotReadGate.requireReadable(snapshot(completeMetadata()),
                        ruleLockWith("card-v99", "events-v4", "protocol-v2")));
        assertEquals(RoomSnapshotReadGate.Failure.STATIC_VERSION_UNSUPPORTED, failure.failure());
        assertEquals(RoomSnapshotReadGate.Failure.STATIC_VERSION_UNSUPPORTED.name(), failure.getMessage());
    }

    @Test
    void rejectsUnsupportedSnapshotSchema() {
        assertFailure(RoomSnapshotReadGate.Failure.SNAPSHOT_SCHEMA_UNSUPPORTED,
                () -> RoomSnapshotReadGate.requireReadable(
                        snapshot(metadata("room-snapshot-v9", "card-v3", "events-v4", "protocol-v2")),
                        ruleLockWith("card-v3", "events-v4", "protocol-v2", "room-snapshot-v9")));
    }

    private static void assertFailure(RoomSnapshotReadGate.Failure expected,
                                      org.junit.jupiter.api.function.Executable executable) {
        RoomSnapshotReadGate.RejectedSnapshotException failure = assertThrows(
                RoomSnapshotReadGate.RejectedSnapshotException.class, executable);
        assertEquals(expected, failure.failure());
        assertEquals(expected.name(), failure.getMessage());
    }

    private static RoomSnapshot snapshot(RoomSnapshotMetadata metadata) {
        return snapshotWithPlayVersion("chengdu1001-v1", metadata);
    }

    private static RoomSnapshot snapshotWithPlayVersion(String playVersion) {
        return snapshotWithPlayVersion(playVersion, completeMetadata());
    }

    private static RoomSnapshot snapshotWithPlayVersion(String playVersion, RoomSnapshotMetadata metadata) {
        return new RoomSnapshot(9, 1001, playVersion, "component-v1", 3, 4, 4,
                Instant.parse("2026-10-07T00:00:00Z"), Map.of("phase", "WAITING"), metadata);
    }

    private static RoomSnapshotRuleLockSource ruleLock() {
        return ruleLockWith("card-v3", "events-v4", "protocol-v2");
    }

    private static RoomSnapshotRuleLockSource ruleLockWith(String cardCodecVersion,
                                                             String eventInterpreterVersion,
                                                             String protocolVersion) {
        return ruleLockWith(cardCodecVersion, eventInterpreterVersion, protocolVersion, "room-snapshot-v1");
    }

    private static RoomSnapshotRuleLockSource ruleLockWith(String cardCodecVersion,
                                                             String eventInterpreterVersion,
                                                             String protocolVersion,
                                                             String schemaVersion) {
        return new RoomSnapshotRuleLockSource(1001, "chengdu1001-v1",
                new CanonicalRoomSnapshotMetadataSource(77L, HASH, HASH, cardCodecVersion,
                        eventInterpreterVersion, protocolVersion, schemaVersion, 12L, "CN-SC", HASH));
    }

    private static RoomSnapshotMetadata completeMetadata() {
        return metadata("room-snapshot-v1", "card-v3", "events-v4", "protocol-v2");
    }

    private static RoomSnapshotMetadata metadata(String schemaVersion,
                                                  String cardCodecVersion,
                                                  String eventInterpreterVersion,
                                                  String protocolVersion) {
        return new RoomSnapshotMetadata(77L, HASH, HASH, cardCodecVersion, eventInterpreterVersion,
                protocolVersion, schemaVersion, 12L, "CN-SC", HASH);
    }

    private static CanonicalRoomSnapshotMetadataSource incompleteSource() {
        return new CanonicalRoomSnapshotMetadataSource(null, HASH, HASH, "card-v3", "events-v4",
                "protocol-v2", "room-snapshot-v1", 12L, "CN-SC", HASH);
    }
}
