package com.aoo.bcg.bootstrap;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Contract guard for the bounded execute metadata wiring. */
final class JdbcGatewayGameCommandCommitterMetadataContractTest {
    @Test
    void executeCommitReadsOneLockedProjectionAndProducesCompleteMetadata() throws Exception {
        String source = Files.readString(Path.of(
                "/Users/aoo/Code/Game/BCG/Aoo/server/modules/infrastructure/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayGameCommandCommitter.java"));
        assertTrue(source.contains("room.requireRoomSnapshotMetadataSource()"));
        assertTrue(source.contains("JOIN aoo_room_rule_lock l"));
        assertTrue(source.contains("JOIN aoo_room_runtime_identity_authority i"));
        assertTrue(source.contains("metadataProducer.produce(lockSource)"));
        assertTrue(source.contains("ROOM_SNAPSHOT_RUNTIME_IDENTITY_CONFLICT"));
        assertTrue(source.contains("ROOM_SNAPSHOT_METADATA_SOURCE_CONFLICT"));
        assertTrue(source.contains("storedIdentity.stateVersion() != stateVersion"));
        assertTrue(source.contains("command result was already committed"));
        assertTrue(source.indexOf("billingLifecycle.requireRoom(connection,room.roomId())") < source.indexOf("if (expectedIdentity != null)"));
        assertFalse(source.contains("aoo_game_release"));
        assertFalse(source.contains("room.gameId() == 0"));
        assertTrue(source.contains("rule_content_hash=VALUES(rule_content_hash)"));
        assertTrue(source.contains("component_content_hash=VALUES(component_content_hash)"));
        assertTrue(source.contains("card_codec_version=VALUES(card_codec_version)"));
        assertTrue(source.contains("event_interpreter_version=VALUES(event_interpreter_version)"));
        assertTrue(source.contains("protocol_version=VALUES(protocol_version)"));
    }

    @Test
    void executeReplayCannotUseTheFormerFixedMetadataLiterals() throws Exception {
        String source = Files.readString(Path.of(
                "/Users/aoo/Code/Game/BCG/Aoo/server/modules/infrastructure/Bootstrap/src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayGameCommandCommitter.java"));
        assertFalse(source.contains("poker-card-v1"));
        assertFalse(source.contains("authority-v1"));
        assertFalse(source.contains("statement.setString(11,\"2.0\")"));
        assertTrue(source.contains("snapshotMetadata.cardCodecVersion()"));
        assertTrue(source.contains("snapshotMetadata.eventInterpreterVersion()"));
        assertTrue(source.contains("snapshotMetadata.protocolVersion()"));
    }
}
