package com.aoo.bcg.common.recovery;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomSnapshotMetadataTest {
    private static final String HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void oneCompleteContractProducesTheCurrentSnapshotColumnProjection() {
        RoomSnapshotMetadata metadata = complete();
        RoomSnapshotMetadata.SnapshotColumns columns = metadata.requireSnapshotColumns();

        assertTrue(metadata.isComplete());
        assertEquals(77L, columns.releaseId());
        assertEquals(HASH, columns.ruleContentHash());
        assertEquals(HASH, columns.componentContentHash());
        assertEquals("card-v3", columns.cardCodecVersion());
        assertEquals("events-v4", columns.eventInterpreterVersion());
        assertEquals("protocol-v2", columns.protocolVersion());

        RoomSnapshot snapshot = new RoomSnapshot(9, 1001, "chengdu1001-v1", "component-v1",
                3, 4, 4, Instant.parse("2026-10-07T00:00:00Z"),
                Map.of("phase", "WAITING"), metadata);
        assertEquals(metadata, snapshot.metadata());
    }

    @Test
    void missingCanonicalMetadataFailsClosedBeforeAnyDurableProjection() {
        RoomSnapshotMetadata missing = RoomSnapshotMetadata.missing();
        assertFalse(missing.isComplete());
        assertThrows(IllegalStateException.class, missing::requireComplete);
        assertThrows(IllegalStateException.class, missing::requireSnapshotColumns);
    }

    @Test
    void invalidSidecarHashCannotBecomeAProjection() {
        assertThrows(IllegalArgumentException.class, () -> new RoomSnapshotMetadata(
                77L, "not-a-hash", HASH, "card-v3", "events-v4", "protocol-v2",
                "room-snapshot-v1", 12L, "CN-SC", HASH));
    }

    static RoomSnapshotMetadata complete() {
        return new RoomSnapshotMetadata(77L, HASH, HASH, "card-v3", "events-v4", "protocol-v2",
                "room-snapshot-v1", 12L, "CN-SC", HASH);
    }
}
