package com.aoo.bcg.common.recovery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CanonicalRoomSnapshotMetadataProducerTest {
    private static final String HASH = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void completeExplicitInputProducesAnImmutableCompleteMetadataValue() {
        CanonicalRoomSnapshotMetadataProducer producer = new CanonicalRoomSnapshotMetadataProducer();
        CanonicalRoomSnapshotMetadataSource source = source();

        RoomSnapshotMetadata first = producer.produce(source);
        RoomSnapshotMetadata second = producer.produce(source);

        assertEquals(first, second);
        assertEquals(new RoomSnapshotMetadata(
                77L, HASH, HASH, "card-v3", "events-v4", "protocol-v2",
                "room-snapshot-v1", 12L, "CN-SC", HASH), first);
        first.requireComplete();
    }

    @Test
    void everyMissingCanonicalFieldFailsClosed() {
        CanonicalRoomSnapshotMetadataProducer producer = new CanonicalRoomSnapshotMetadataProducer();
        assertThrows(IllegalStateException.class, () -> producer.produce(null));
        assertThrows(IllegalStateException.class, () -> producer.produce(with(source(), null, null, null, null, null, null, null, null, null, null)));
        assertThrows(IllegalStateException.class, () -> producer.produce(with(source(), 0L, HASH, HASH, "card-v3", "events-v4", "protocol-v2", "room-snapshot-v1", 12L, "CN-SC", HASH)));
        assertThrows(IllegalStateException.class, () -> producer.produce(with(source(), 77L, null, HASH, "card-v3", "events-v4", "protocol-v2", "room-snapshot-v1", 12L, "CN-SC", HASH)));
        assertThrows(IllegalStateException.class, () -> producer.produce(with(source(), 77L, HASH, null, "card-v3", "events-v4", "protocol-v2", "room-snapshot-v1", 12L, "CN-SC", HASH)));
        assertThrows(IllegalStateException.class, () -> producer.produce(with(source(), 77L, HASH, HASH, null, "events-v4", "protocol-v2", "room-snapshot-v1", 12L, "CN-SC", HASH)));
        assertThrows(IllegalStateException.class, () -> producer.produce(with(source(), 77L, HASH, HASH, "card-v3", null, "protocol-v2", "room-snapshot-v1", 12L, "CN-SC", HASH)));
        assertThrows(IllegalStateException.class, () -> producer.produce(with(source(), 77L, HASH, HASH, "card-v3", "events-v4", null, "room-snapshot-v1", 12L, "CN-SC", HASH)));
        assertThrows(IllegalStateException.class, () -> producer.produce(with(source(), 77L, HASH, HASH, "card-v3", "events-v4", "protocol-v2", null, 12L, "CN-SC", HASH)));
        assertThrows(IllegalStateException.class, () -> producer.produce(with(source(), 77L, HASH, HASH, "card-v3", "events-v4", "protocol-v2", "room-snapshot-v1", null, "CN-SC", HASH)));
        assertThrows(IllegalStateException.class, () -> producer.produce(with(source(), 77L, HASH, HASH, "card-v3", "events-v4", "protocol-v2", "room-snapshot-v1", 12L, null, HASH)));
        assertThrows(IllegalStateException.class, () -> producer.produce(with(source(), 77L, HASH, HASH, "card-v3", "events-v4", "protocol-v2", "room-snapshot-v1", 12L, "CN-SC", null)));
    }

    @Test
    void conflictingExplicitSourcesFailClosedBeforeConstruction() {
        CanonicalRoomSnapshotMetadataProducer producer = new CanonicalRoomSnapshotMetadataProducer();
        CanonicalRoomSnapshotMetadataSource conflict = with(source(), 78L, HASH, HASH, "card-v3", "events-v4", "protocol-v2", "room-snapshot-v1", 12L, "CN-SC", HASH);

        assertThrows(IllegalStateException.class, () -> producer.produce(source(), conflict));
    }

    private static CanonicalRoomSnapshotMetadataSource source() {
        return new CanonicalRoomSnapshotMetadataSource(
                77L, HASH, HASH, "card-v3", "events-v4", "protocol-v2",
                "room-snapshot-v1", 12L, "CN-SC", HASH);
    }

    private static CanonicalRoomSnapshotMetadataSource with(
            CanonicalRoomSnapshotMetadataSource ignored,
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
        return new CanonicalRoomSnapshotMetadataSource(
                releaseId, ruleContentHash, componentContentHash, cardCodecVersion,
                eventInterpreterVersion, protocolVersion, snapshotSchemaVersion,
                indexGeneration, regionCode, sourceHash);
    }
}
