package com.aoo.bcg.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomConfigurationSnapshotMetadataProviderTest {
    private static final String HASH = "a".repeat(64);
    private JdbcDataSource dataSource;

    @BeforeEach
    void createRoomLockProjection() throws Exception {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:room-lock-metadata;MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS aoo_room_rule_lock");
            statement.executeUpdate("CREATE TABLE aoo_room_rule_lock ("
                    + "room_id BIGINT PRIMARY KEY, game_id INT NOT NULL, region_code VARCHAR(32) NOT NULL,"
                    + "play_version VARCHAR(64) NOT NULL, index_generation BIGINT NOT NULL, release_id BIGINT NOT NULL,"
                    + "rule_hash CHAR(64) NOT NULL, component_hash CHAR(64) NOT NULL,"
                    + "card_codec_version VARCHAR(64) NOT NULL, event_interpreter_version VARCHAR(64) NOT NULL,"
                    + "protocol_version VARCHAR(64) NOT NULL, snapshot_schema_version VARCHAR(64), source_hash CHAR(64),"
                    + "immutable_rules CLOB NOT NULL, created_at TIMESTAMP NOT NULL)");
        }
    }

    @Test
    void readsAllCanonicalMetadataFromRoomLockOnly() throws Exception {
        insert(7, "snapshot-7", HASH, "{}");
        var store = store();
        var provider = new RoomConfigurationSnapshotMetadataProvider(store);

        var source = provider.require(7);
        assertEquals(91L, source.releaseId());
        assertEquals(HASH, source.ruleContentHash());
        assertEquals(HASH, source.componentContentHash());
        assertEquals("codec-7", source.cardCodecVersion());
        assertEquals("events-7", source.eventInterpreterVersion());
        assertEquals("protocol-7", source.protocolVersion());
        assertEquals("snapshot-7", source.snapshotSchemaVersion());
        assertEquals(17L, source.indexGeneration());
        assertEquals("CN-51-01", source.regionCode());
        assertEquals(HASH, source.sourceHash());
    }

    @Test
    void legacySnapshotReadStillFailsClosedWhenScoreVersionIsMissing() throws Exception {
        insert(10, "snapshot-10", HASH, "{}");
        var error = assertThrows(IllegalStateException.class, () -> store().find(10));
        assertEquals("ROOM_CONFIGURATION_LOCK_SCORE_VERSION_REQUIRED", error.getMessage());
    }

    @Test
    void incompleteHistoricalLockFailsClosed() throws Exception {
        insert(8, null, HASH, "{}");
        var error = assertThrows(IllegalArgumentException.class,
                () -> new RoomConfigurationSnapshotMetadataProvider(store()).find(8));
        assertTrue(error.getMessage().contains("SCHEMA_REQUIRED"));
    }

    @Test
    void missingExplicitSourceAtCreationFailsClosed() {
        var error = assertThrows(IllegalStateException.class,
                () -> store().open(9, 1001, "CN-51-01", "chengdu1001-v1.0.0"));
        assertEquals("ROOM_CONFIGURATION_LOCK_METADATA_SOURCE_REQUIRED", error.getMessage());
    }

    @Test
    void explicitSchemaWithoutRuntimeVersionSourceFailsClosed() {
        var error = assertThrows(IllegalStateException.class,
                () -> store().open(9, 1001, "CN-51-01", "chengdu1001-v1.0.0", HASH, "snapshot-9"));
        assertEquals("ROOM_CONFIGURATION_LOCK_RUNTIME_METADATA_SOURCE_REQUIRED", error.getMessage());
    }

    @Test
    void invalidSourceAndSchemaAreRejectedBeforePersistence() {
        assertThrows(IllegalArgumentException.class,
                () -> new RoomConfigurationLeaseStore.RoomLockMetadata(
                        91, 1001, "CN-51-01", "chengdu1001-v1.0.0", 17,
                        HASH, HASH, "codec-7", "events-7", "protocol-7", "", HASH));
        assertThrows(IllegalArgumentException.class,
                () -> new RoomConfigurationLeaseStore.RoomLockMetadata(
                        91, 1001, "CN-51-01", "chengdu1001-v1.0.0", 17,
                        HASH, HASH, "codec-7", "events-7", "protocol-7", "snapshot-7", "not-a-hash"));
    }

    private RoomConfigurationLeaseStore store() {
        return new RoomConfigurationLeaseStore(dataSource, new ObjectMapper());
    }

    private void insert(long roomId, String schema, String sourceHash, String rules) throws Exception {
        String sql = "INSERT INTO aoo_room_rule_lock(room_id,game_id,region_code,play_version,index_generation,"
                + "release_id,rule_hash,component_hash,card_codec_version,event_interpreter_version,protocol_version,"
                + "snapshot_schema_version,source_hash,immutable_rules,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection connection = dataSource.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId);
            statement.setInt(2, 1001);
            statement.setString(3, "CN-51-01");
            statement.setString(4, "chengdu1001-v1.0.0");
            statement.setLong(5, 17);
            statement.setLong(6, 91);
            statement.setString(7, HASH);
            statement.setString(8, HASH);
            statement.setString(9, "codec-7");
            statement.setString(10, "events-7");
            statement.setString(11, "protocol-7");
            statement.setString(12, schema);
            statement.setString(13, sourceHash);
            statement.setString(14, rules);
            statement.setTimestamp(15, Timestamp.from(Instant.parse("2026-10-07T00:00:00Z")));
            statement.executeUpdate();
        }
    }
}
