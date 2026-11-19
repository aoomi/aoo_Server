package com.aoo.bcg.common.recovery;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcRoomSnapshotStoreSidecarContractTest {
    @Test
    void snapshotStoreUsesTheExactSixColumnProjectionForInsertAndUpdate() throws Exception {
        String source = Files.readString(sourcePath());
        for (String field : new String[]{"rule_content_hash", "component_content_hash",
                "card_codec_version", "event_interpreter_version", "protocol_version"}) {
            assertTrue(source.contains(field), "missing sidecar field: " + field);
        }
        assertTrue(source.contains("metadata.requireSnapshotColumns()"));
        assertTrue(source.contains("columns.ruleContentHash()"));
        assertTrue(source.contains("columns.componentContentHash()"));
        assertTrue(source.contains("columns.cardCodecVersion()"));
        assertTrue(source.contains("columns.eventInterpreterVersion()"));
        assertTrue(source.contains("columns.protocolVersion()"));
        assertTrue(source.contains("rule_content_hash=VALUES(rule_content_hash)"));
        assertTrue(source.contains("component_content_hash=VALUES(component_content_hash)"));
        assertTrue(source.contains("card_codec_version=VALUES(card_codec_version)"));
        assertTrue(source.contains("event_interpreter_version=VALUES(event_interpreter_version)"));
        assertTrue(source.contains("protocol_version=VALUES(protocol_version)"));
    }

    @Test
    void readsNeverInventSnapshotSchemaMetadata() throws Exception {
        String source = Files.readString(sourcePath());
        assertTrue(source.contains("l.source_hash,l.snapshot_schema_version"));
        assertTrue(source.contains("result.getString(19), nullableLong(result, 16), result.getString(17), result.getString(18)"));
        assertFalse(source.contains("l.bundle_hash"));
        assertFalse(source.contains("null, nullableLong(result, 16)"));
        assertFalse(source.contains("room-snapshot-v1"));
    }

    @Test
    void eventJournalUsesTheSameProjectionAndUpdatesEveryColumn() throws Exception {
        String source = Files.readString(journalPath());
        assertTrue(source.contains("snapshot.metadata().requireSnapshotColumns()"));
        assertTrue(source.contains("RoomSnapshotMetadata.SnapshotColumns"));
        for (String field : new String[]{"rule_content_hash", "component_content_hash",
                "card_codec_version", "event_interpreter_version", "protocol_version"}) {
            assertTrue(source.contains(field), "missing event sidecar field: " + field);
        }
        assertTrue(source.contains("rule_content_hash=?"));
        assertTrue(source.contains("component_content_hash=?"));
        assertTrue(source.contains("card_codec_version=?"));
        assertTrue(source.contains("event_interpreter_version=?"));
        assertTrue(source.contains("protocol_version=?"));
    }

    private static Path sourcePath() {
        Path fromRoot = Path.of("server/CommonRoom/src/main/java/com/aoo/bcg/common/recovery/JdbcRoomSnapshotStore.java");
        if (Files.isRegularFile(fromRoot)) return fromRoot;
        return Path.of("src/main/java/com/aoo/bcg/common/recovery/JdbcRoomSnapshotStore.java");
    }

    private static Path journalPath() {
        Path fromRoot = Path.of("server/CommonRoom/src/main/java/com/aoo/bcg/common/event/JdbcRoomEventJournal.java");
        if (Files.isRegularFile(fromRoot)) return fromRoot;
        return Path.of("src/main/java/com/aoo/bcg/common/event/JdbcRoomEventJournal.java");
    }
}
