package com.aoo.bcg.bootstrap;

import com.aoo.bcg.config.RoomRuntimeIdentityStore;
import com.aoo.bcg.gamespi.*;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

final class JdbcGatewayRoomAuthorityReadyIdempotencyTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String H1 = "a".repeat(64), H2 = "b".repeat(64), H3 = "c".repeat(64);

    @Test
    void readyOwnsTheDurableLeaseAndProcessReplayDoesNotMutateTwice() throws Exception {
        try (Fixture fixture = fixture(false)) {
            RoomOrchestrator orchestrator = orchestrator(fixture.authority);
            RoomOrchestrator.ReadyResult first = orchestrator.ready(fixture.command());
            RoomOrchestrator.ReadyResult replay = orchestrator.ready(fixture.command());

            assertEquals(first, replay);
            assertEquals(1, fixture.mutations.get());
            assertEquals(5L, first.expectedNextStateVersion());
            assertEquals(List.of("COMPLETED", 5L, 5L), durable(fixture.dataSource));
        }
    }

    @Test
    void readyMarksTheLeaseUnknownAfterAuthorityMutationFails() throws Exception {
        try (Fixture fixture = fixture(true)) {
            IllegalStateException failure = assertThrows(IllegalStateException.class,
                    () -> orchestrator(fixture.authority).ready(fixture.command()));
            assertEquals("synthetic mutation failure", failure.getMessage());
            assertEquals(1, fixture.mutations.get());
            assertEquals(List.of("UNKNOWN", 4L, 4L), durable(fixture.dataSource));
        }
    }

    private static RoomOrchestrator orchestrator(JdbcGatewayRoomAuthority authority) throws Exception {
        Field field = JdbcGatewayRoomAuthority.class.getDeclaredField("roomOrchestrator");
        field.setAccessible(true);
        return (RoomOrchestrator) field.get(authority);
    }

    private static Fixture fixture(boolean failMutation) throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:ready_lease_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        for (String statement : SCHEMA.split(";")) if (!statement.isBlank()) sql(dataSource, statement);
        sql(dataSource, "INSERT INTO aoo_room_authority_route(room_id,game_id,play_version,last_request_id,trace_id,node_id,lifecycle_state,lease_expires_at,fencing_token) VALUES(42,2003,'liangshan2003-v1.0.0','create','trace','node','ACTIVE',DATEADD('HOUR',1,CURRENT_TIMESTAMP),11)");
        sql(dataSource, "INSERT INTO aoo_hall_room VALUES(42,2003,'liangshan2003-v1.0.0','LS',11,17,'{}','WAITING')");
        sql(dataSource, "INSERT INTO aoo_game_catalog VALUES(2003,'liangshan2003')");
        sql(dataSource, "INSERT INTO aoo_room_rule_lock(room_id,game_id,region_code,play_version,index_generation,release_id,rule_hash,component_hash,card_codec_version,event_interpreter_version,protocol_version,snapshot_schema_version,source_hash,immutable_rules) VALUES(42,2003,'LS','liangshan2003-v1.0.0',1,11,'" + H1 + "','" + H2 + "','codec-v1','events-v1','protocol-v1','snapshot-v1','" + H3 + "','{}')");
        sql(dataSource, "INSERT INTO aoo_room_snapshot(room_id,fencing_token,state_version,last_event_sequence,state_payload) VALUES(42,11,4,4,'{}')");
        RoomRuntimeIdentity identity = new RoomRuntimeIdentity("instance-42", 7, 11, 4);
        new RoomRuntimeIdentityStore(dataSource).create(42, identity);
        JdbcRoomBillingLifecyclePortTest.seed(dataSource, 42, 17, 2003,
                "liangshan2003-v1.0.0", "LS", identity);

        AtomicLong version = new AtomicLong(4);
        AtomicInteger mutations = new AtomicInteger();
        AuthoritativeGameSession session = (AuthoritativeGameSession) Proxy.newProxyInstance(
                JdbcGatewayRoomAuthorityReadyIdempotencyTest.class.getClassLoader(),
                new Class[]{AuthoritativeGameSession.class}, (proxy, method, args) -> switch (method.getName()) {
                    case "stateVersion" -> version.get();
                    case "operationDeadline" -> OperationDeadline.none();
                    case "authoritativeState" -> Map.of("stateVersion", version.get(), "phase", "WAITING",
                            "roundNo", 0, "players", Map.of(1, 17L));
                    case "execute" -> {
                        mutations.incrementAndGet();
                        if (failMutation) throw new IllegalStateException("synthetic mutation failure");
                        version.incrementAndGet();
                        GameCommandRequest request = (GameCommandRequest) args[0];
                        yield new GameCommandResult("common.room.ready_resp", request.requestId(),
                                CommandPayload.copyOf(Map.of("ready", true)));
                    }
                    default -> throw new AssertionError("unexpected session call " + method.getName());
                });
        GameRoomHandle room = new GameRoomHandle(42, 2003, "liangshan2003-v1.0.0", session,
                null, new RoomSnapshotMetadataSource(11L, H1, H2, "codec-v1", "events-v1",
                "protocol-v1", "snapshot-v1", 1L, "LS", H3), identity);
        var rooms = new com.aoo.bcg.gateway.RuntimeGameRoomRegistry();
        rooms.bindRuntime(room);
        var games = GameRegistryFactory.production();
        games.register(new CatalogGameProvider(new GameDescriptor(2003, "liangshan2003",
                "fixture", GameCategory.POKER, "poker", RegionScope.CITY, "SC", "LS",
                "liangshan2003-v1.0.0"), true, "fixture"));
        Clock clock = Clock.systemUTC();
        var authority = new JdbcGatewayRoomAuthority(dataSource, games, rooms, clock, "node",
                "unit-test://authority", JSON, JdbcGatewayRoomAuthorityAtomicCommitTest.committer(dataSource),
                new com.aoo.bcg.gateway.GatewayRoomBroadcastHub((roomId, account) -> {
                    throw new AssertionError("broadcast outside test");
                }, JSON, clock), new HallRoomLifecycleClient(java.net.URI.create("unit-test://hall"),
                "synthetic-only-not-live-token-xxxxxxxx", JSON));
        return new Fixture(dataSource, authority, mutations);
    }

    private static List<Object> durable(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); var statement = connection.createStatement();
             var row = statement.executeQuery("SELECT b.status,i.state_version,s.state_version FROM aoo_business_idempotency b JOIN aoo_room_runtime_identity_authority i ON i.room_id=b.room_id JOIN aoo_room_snapshot s ON s.room_id=b.room_id WHERE b.room_id=42")) {
            assertTrue(row.next());
            return List.of(row.getString(1), row.getLong(2), row.getLong(3));
        }
    }

    private static void sql(DataSource dataSource, String statement) throws Exception {
        try (Connection connection = dataSource.getConnection(); var sql = connection.createStatement()) {
            sql.execute(statement);
        }
    }

    private record Fixture(JdbcDataSource dataSource, JdbcGatewayRoomAuthority authority,
                           AtomicInteger mutations) implements AutoCloseable {
        RoomOrchestrator.ReadyCommand command() {
            var identity = new RoomOrchestrator.RoomIdentity(42, "instance-42", 2003,
                    "liangshan2003", "liangshan2003-v1.0.0", 7, 11, 4);
            return new RoomOrchestrator.ReadyCommand(identity,
                    new RoomOrchestrator.RoomStaticIdentity(42, 2003, "liangshan2003",
                            "liangshan2003-v1.0.0"), 17, 1,
                    RoomOrchestrator.ReadyPermission.SEATED_MEMBER, RoomState.WAITING,
                    "ready-17", CommandPayload.copyOf(Map.of()));
        }

        @Override public void close() throws Exception {
            authority.close();
            sql(dataSource, "SHUTDOWN");
        }
    }

    private static final String SCHEMA = """
            CREATE TABLE aoo_room_runtime_identity_authority(room_id BIGINT PRIMARY KEY,room_instance_id VARCHAR(128),lifecycle_epoch BIGINT,fencing_token BIGINT,state_version BIGINT,lifecycle_state VARCHAR(16),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
            CREATE TABLE aoo_room_authority_route(room_id BIGINT PRIMARY KEY,game_id INT,play_version VARCHAR(100),last_request_id VARCHAR(100),trace_id VARCHAR(100),node_id VARCHAR(100),lifecycle_state VARCHAR(100),lease_expires_at TIMESTAMP,fencing_token BIGINT,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,first_round_started_at TIMESTAMP,last_business_activity_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
            CREATE TABLE aoo_hall_room(room_id BIGINT PRIMARY KEY,game_id INT,play_version VARCHAR(100),classification_region_code VARCHAR(100),release_id BIGINT,owner_account_id BIGINT,rules_json CLOB,state VARCHAR(100));
            CREATE TABLE aoo_game_catalog(game_id INT,game_code VARCHAR(100));
            CREATE TABLE aoo_room_rule_lock(room_id BIGINT,game_id INT,region_code VARCHAR(100),play_version VARCHAR(100),index_generation BIGINT,release_id BIGINT,rule_hash VARCHAR(64),component_hash VARCHAR(64),card_codec_version VARCHAR(100),event_interpreter_version VARCHAR(100),protocol_version VARCHAR(100),snapshot_schema_version VARCHAR(100),source_hash VARCHAR(64),immutable_rules CLOB,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);
            CREATE TABLE aoo_room_snapshot(room_id BIGINT PRIMARY KEY,game_id INT,play_version VARCHAR(100),release_id BIGINT,component_version VARCHAR(100),rule_content_hash VARCHAR(64),component_content_hash VARCHAR(64),card_codec_version VARCHAR(100),event_interpreter_version VARCHAR(100),protocol_version VARCHAR(100),fencing_token BIGINT,state_version BIGINT,last_event_sequence BIGINT,captured_at TIMESTAMP,state_payload CLOB);
            CREATE TABLE aoo_business_idempotency(request_id VARCHAR(1024) PRIMARY KEY,user_id VARCHAR(100),operation VARCHAR(100),room_id BIGINT,round_no INT,client_request_id VARCHAR(100),status VARCHAR(100),response_code INT,response_version VARCHAR(100),response_schema_version INT,response_payload CLOB,expires_at TIMESTAMP,request_hash VARCHAR(64),created_at TIMESTAMP);
            CREATE ALIAS SHA2 FOR "com.aoo.bcg.bootstrap.JdbcGatewayRoomAuthorityAtomicCommitTest.sha2";
            """;
}
