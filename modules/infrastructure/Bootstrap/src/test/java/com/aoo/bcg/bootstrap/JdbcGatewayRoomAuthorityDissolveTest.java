package com.aoo.bcg.bootstrap;

import com.aoo.bcg.common.authority.JdbcRoomDissolveAuthorityStore;
import com.aoo.bcg.gamespi.RoomOrchestrator;
import com.aoo.bcg.gamespi.RoomRuntimeIdentity;
import com.aoo.bcg.gamespi.RoomSnapshotMetadataSource;
import com.aoo.bcg.gamespi.RoomState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcGatewayRoomAuthorityDissolveTest {
    private static final Instant NOW = Instant.parse("2050-01-01T00:00:00Z");
    private static final RoomOrchestrator.RoomIdentity IDENTITY =
            new RoomOrchestrator.RoomIdentity(42L, "instance-42", 2003,
                    "liangshan2003", "liangshan2003-v1", 7L, 11L, 0L);
    private static final RoomOrchestrator.RoomStaticIdentity STATIC =
            new RoomOrchestrator.RoomStaticIdentity(42L, 2003,
                    "liangshan2003", "liangshan2003-v1");
    private static final RoomSnapshotMetadataSource METADATA = new RoomSnapshotMetadataSource(
            19L, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb",
            "card-v1", "events-v1", "protocol-v2", "snapshot-v1", 4L, "LS",
            "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");

    private DataSource dataSource;
    private JdbcRoomDissolveAuthorityStore store;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:dissolve_" + System.nanoTime() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource = source;
        try (Connection connection = dataSource.getConnection(); Statement sql = connection.createStatement()) {
            sql.execute("CREATE TABLE aoo_room_runtime_identity_authority ("
                    + "room_id BIGINT PRIMARY KEY, room_instance_id VARCHAR(128), lifecycle_epoch BIGINT,"
                    + "fencing_token BIGINT, state_version BIGINT, lifecycle_state VARCHAR(16),"
                    + "updated_at TIMESTAMP)");
            sql.execute("CREATE TABLE aoo_room_rule_lock (room_id BIGINT PRIMARY KEY, game_id BIGINT,"
                    + "region_code VARCHAR(32), play_version VARCHAR(64), index_generation BIGINT,"
                    + "release_id BIGINT, rule_hash CHAR(64), component_hash CHAR(64),"
                    + "card_codec_version VARCHAR(64), event_interpreter_version VARCHAR(64),"
                    + "protocol_version VARCHAR(64), source_hash CHAR(64), snapshot_schema_version VARCHAR(64))");
            sql.execute("CREATE TABLE aoo_room_dissolve_authority ("
                    + "room_id BIGINT, room_instance_id VARCHAR(128), lifecycle_epoch BIGINT,"
                    + "fencing_token BIGINT, state_version BIGINT, expected_state_version BIGINT,"
                    + "vote_id VARCHAR(128), applicant_account_id BIGINT, eligible_member_ids VARCHAR(2000),"
                    + "deadline_at TIMESTAMP, approve_on_timeout BOOLEAN, votes VARCHAR(2000),"
                    + "decision VARCHAR(16), terminal_tombstone BOOLEAN, terminal_at TIMESTAMP,"
                    + "PRIMARY KEY(room_id,lifecycle_epoch,vote_id))");
            sql.execute("CREATE TABLE aoo_room_dissolve_command_receipt ("
                    + "room_id BIGINT, lifecycle_epoch BIGINT, command_id VARCHAR(128),"
                    + "idempotency_key VARCHAR(128), operation VARCHAR(16), request_hash CHAR(64),"
                    + "result_status VARCHAR(16), vote_id VARCHAR(128), applicant_account_id BIGINT,"
                    + "eligible_member_ids VARCHAR(2000), deadline_at TIMESTAMP, approve_on_timeout BOOLEAN,"
                    + "votes VARCHAR(2000), decision VARCHAR(16), result_state_version BIGINT,"
                    + "terminal_tombstone BOOLEAN, PRIMARY KEY(room_id,lifecycle_epoch,command_id),"
                    + "UNIQUE(room_id,lifecycle_epoch,idempotency_key))");
            sql.execute("INSERT INTO aoo_room_runtime_identity_authority VALUES"
                    + "(42,'instance-42',7,11,0,'ACTIVE',CURRENT_TIMESTAMP)");
            sql.execute("INSERT INTO aoo_room_rule_lock VALUES"
                    + "(42,2003,'LS','liangshan2003-v1',4,19,'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',"
                    + "'bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb',"
                    + "'card-v1','events-v1','protocol-v2',"
                    + "'cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc','snapshot-v1')");
        }
        store = new JdbcRoomDissolveAuthorityStore(dataSource, new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void applyVoteReplayAndRecoveryUseDurableStateAndExactReceipt() {
        RoomOrchestrator.DissolveApplyCommand apply = apply("apply-1", "idem-1", IDENTITY);
        RoomOrchestrator.DissolveResult applied = store.apply(apply);
        assertEquals(1L, applied.identity().stateVersion());
        RoomOrchestrator.DissolveResult replay = store.apply(apply);
        assertEquals(applied, replay);

        RoomOrchestrator.RoomIdentity afterApply = identity(1L);
        RoomOrchestrator.DissolveVoteCommand vote = new RoomOrchestrator.DissolveVoteCommand(
                afterApply, STATIC, "session-42", 18L,
                RoomOrchestrator.DissolvePermission.ELIGIBLE_MEMBER, RoomState.PLAYING,
                "vote-42", true, 1L, "vote-1", "vote-idem-1", METADATA);
        RoomOrchestrator.DissolveResult voted = store.vote(vote);
        assertEquals(RoomOrchestrator.DissolveDecision.APPROVED, voted.decision());
        assertTrue(voted.terminalTombstone());
        assertEquals(2L, voted.identity().stateVersion());

        RoomOrchestrator.DissolveResult recovered = store.recover(new RoomOrchestrator.DissolveRecoverCommand(
                identity(2L), STATIC, "vote-42", METADATA));
        assertEquals(RoomOrchestrator.DissolveStatus.RECOVERED, recovered.status());
        assertEquals(RoomOrchestrator.DissolveDecision.APPROVED, recovered.decision());
        assertTrue(recovered.terminalTombstone());
    }

    @Test
    void staleGenerationPermissionAndReceiptConflictsFailClosed() {
        store.apply(apply("apply-2", "idem-2", IDENTITY));
        RoomOrchestrator.DissolveApplyCommand conflicting = apply("apply-2", "other-idem", IDENTITY);
        assertThrows(SecurityException.class, () -> store.apply(conflicting));

        RoomOrchestrator.DissolveVoteCommand denied = new RoomOrchestrator.DissolveVoteCommand(
                identity(1L), STATIC, "session-42", 18L,
                RoomOrchestrator.DissolvePermission.DENIED, RoomState.PLAYING,
                "vote-42", true, 1L, "vote-denied", "vote-denied-idem", METADATA);
        assertThrows(SecurityException.class, () -> store.vote(denied));

        RoomOrchestrator.RoomIdentity stale = new RoomOrchestrator.RoomIdentity(42L,
                "other-instance", 2003, "liangshan2003", "liangshan2003-v1", 7L, 11L, 0L);
        assertThrows(SecurityException.class, () -> store.apply(apply("apply-3", "idem-3", stale)));
    }

    @Test
    void migrationIsAdditiveAndRollbackIsExplicitlyNotAutomatic() throws Exception {
        String migration = Files.readString(Path.of("..", "..", "database", "migrations",
                "V20261008_04__room_dissolve_authority.sql"));
        assertTrue(migration.contains("CREATE TABLE aoo_room_dissolve_authority"));
        assertTrue(migration.contains("CREATE TABLE aoo_room_dissolve_command_receipt"));
        assertTrue(migration.contains("Manual rollback record"));
        assertTrue(migration.contains("Once any"));
        assertTrue(migration.contains("production row exists, rollback is forbidden"));
    }

    private RoomOrchestrator.DissolveApplyCommand apply(String commandId, String idempotencyKey,
                                                         RoomOrchestrator.RoomIdentity identity) {
        return new RoomOrchestrator.DissolveApplyCommand(identity, STATIC, "session-42", 17L,
                RoomOrchestrator.DissolvePermission.APPLICANT, RoomState.PLAYING, "vote-42",
                Set.of(17L, 18L), NOW.plusSeconds(60), true, true,
                identity.stateVersion(), commandId, idempotencyKey, METADATA);
    }

    private RoomOrchestrator.RoomIdentity identity(long stateVersion) {
        return new RoomOrchestrator.RoomIdentity(42L, "instance-42", 2003,
                "liangshan2003", "liangshan2003-v1", 7L, 11L, stateVersion);
    }
}
