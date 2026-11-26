package com.aoo.bcg.bootstrap;

import com.aoo.bcg.billing.JdbcRoomBillingIntentJournal;
import com.aoo.bcg.billing.JdbcRoomBillingSourceBindingRepository;
import com.aoo.bcg.billing.JdbcRoomChargeCoordinator;
import com.aoo.bcg.billing.RoomBillingIntent;
import com.aoo.bcg.billing.RoomBillingSourceBinding;
import com.aoo.bcg.billing.RoomChargeOperation;
import com.aoo.bcg.config.RoomRuntimeIdentityStore;
import com.aoo.bcg.gamespi.AuthoritativeGameSession;
import com.aoo.bcg.gamespi.GameRegistryFactory;
import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.GameRoomHandle;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
import com.aoo.bcg.gamespi.RoomOrchestrator;
import com.aoo.bcg.gamespi.RoomRuntimeIdentity;
import com.aoo.bcg.gamespi.RoomSnapshotMetadataSource;
import com.aoo.bcg.gateway.GatewayRoomBroadcastHub;
import com.aoo.bcg.gateway.RuntimeGameRoomRegistry;
import com.aoo.bcg.poker.PokerCatalogRuntimeRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.h2.jdbcx.JdbcDataSource;
import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JdbcGatewayRoomAuthorityProviderDispatchTest {
    private static final long ROOM_ID = 42L;
    private static final String PLAY_VERSION = "liangshan2003-v1.0.0";
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String RULE_HASH = "a".repeat(64);
    private static final String COMPONENT_HASH = "b".repeat(64);
    private static final String SOURCE_HASH = "c".repeat(64);

    @Test
    void realLiangshanProviderUnwrapsOuterDispatchBeforeMutatingAuthoritySession() {
        GameProvider provider = liangshanProvider();
        GameRoomHandle room = provider.roomFactory().create(new RoomCreationContext(
                ROOM_ID, 10L, Map.of(
                        "playerCount", 2,
                        "roundCount", 8,
                        "dealCardCount", 10,
                        "robDealerRule", "dealer_first")));
        AuthoritativeGameSession session = room.requireAuthoritativeSession();

        session.execute(inner("join_req", "join-1", 1, 11L, 1, Map.of()));
        session.execute(inner("ready_req", "ready-0", 2, 10L, 0, Map.of()));
        session.execute(inner("ready_req", "ready-1", 3, 11L, 1, Map.of()));
        assertEquals("COMPETE_DEALER", session.viewFor(10L).get("phase"));

        int currentSeat = ((Number) session.viewFor(10L).get("currentSeat")).intValue();
        long actor = ((Number) ((Map<?, ?>) session.authoritativeState().get("players"))
                .get(currentSeat)).longValue();
        long before = session.stateVersion();
        GameCommandRequest wrongBusiness = dispatch("poker.chengdu2001.dispatch", "wrong-business",
                before + 1, actor, currentSeat);
        assertThrows(SecurityException.class,
                () -> provider.commandHandler().orElseThrow().handle(room, wrongBusiness));
        assertEquals(before, session.stateVersion(), "business identity must reject before mutation");

        GameCommandRequest request = dispatch("poker.liangshan2003.dispatch", "rob-dealer-1",
                before + 1, actor, currentSeat);
        var result = provider.commandHandler().orElseThrow().handle(room, request);

        assertEquals("rob-dealer-1", result.requestId());
        assertEquals(before + 1, session.stateVersion());
        assertEquals("poker.liangshan2003.dispatch_resp", result.msgId());
        assertEquals("robDealer", result.body().get("action"));
        assertEquals(true, session.viewFor(actor).get("cardsDealt"));
        assertEquals("PLAYING", session.viewFor(actor).get("phase"),
                "outer dispatch must execute the provider's rob_dealer operation");

        long committed = session.stateVersion();
        assertThrows(RuntimeException.class,
                () -> provider.commandHandler().orElseThrow().handle(room, request));
        assertEquals(committed, session.stateVersion(),
                "same request must not advance the provider session twice");
    }

    @Test
    void catalogProviderStillOwnsItsCommandHandler() {
        GameProvider provider = liangshanProvider();
        assertEquals(2003, provider.descriptor().gameId());
        assertEquals(PLAY_VERSION, provider.descriptor().version());
        assertEquals(true, provider.commandHandler().isPresent());
    }

    @Test
    void authorityExecuteCommitsProviderDispatchAndReplaysDurableReceipt() throws Exception {
        try (AuthorityFixture fixture = authorityFixture()) {
            long before = fixture.session.stateVersion();
            int currentSeat = currentSeat(fixture.session);
            long actor = actorAt(fixture.session, currentSeat);
            GameCommandRequest request = dispatch("poker.liangshan2003.dispatch", "authority-rob-dealer-1",
                    before + 1, actor, currentSeat);

            RoomOrchestrator.ExecuteResult first = fixture.authority.execute(
                    command(identity(before), request));

            assertEquals(before + 1, first.identity().stateVersion());
            assertEquals("robDealer", first.commandResult().body().get("action"));
            assertEquals("COMPETE_DEALER", fixture.session.viewFor(actor).get("phase"),
                    "one real robDealer action must remain in the multi-player compete phase");
            assertEquals(before + 1, persistedVersion(fixture.db, "aoo_room_runtime_identity_authority"));
            assertEquals(before + 1, persistedVersion(fixture.db, "aoo_room_snapshot"));
            assertEquals(1, count(fixture.db, "aoo_business_idempotency"));

            GameCommandRequest wrongBusiness = dispatch("poker.chengdu2001.dispatch",
                    request.requestId(), request.sequence(), actor, currentSeat);
            assertThrows(SecurityException.class,
                    () -> fixture.authority.execute(command(first.identity(), wrongBusiness)));
            long wrongActorId = actor == 10L ? 11L : 10L;
            int wrongActorSeat = wrongActorId == 10L ? 0 : 1;
            GameCommandRequest wrongActor = dispatch(request.msgId(), request.requestId(),
                    request.sequence(), wrongActorId, wrongActorSeat);
            assertThrows(RuntimeException.class,
                    () -> fixture.authority.execute(command(first.identity(), wrongActor)));
            assertEquals(before + 1, fixture.session.stateVersion(),
                    "non-matching business or actor must not obtain or mutate the committed result");
            assertEquals(before + 1, persistedVersion(fixture.db, "aoo_room_snapshot"));

            RoomOrchestrator.ExecuteResult replay = fixture.authority.execute(
                    command(first.identity(), request));

            assertEquals(first.identity(), replay.identity());
            assertEquals(first.commandResult().msgId(), replay.commandResult().msgId());
            assertEquals(first.commandResult().requestId(), replay.commandResult().requestId());
            assertEquals(first.commandResult().committedBinding(), replay.commandResult().committedBinding(),
                    "same requestId must return the exact committed authority binding");
            assertEquals(first.commandResult().serverTimeEpochMillis(),
                    replay.commandResult().serverTimeEpochMillis());
            assertEquals(first.commandResult().operationDeadline(),
                    replay.commandResult().operationDeadline());
            assertEquals(first.commandResult().body().get("action"),
                    replay.commandResult().body().get("action"));
            assertEquals(((Number) first.commandResult().body().get("stateVersion")).longValue(),
                    ((Number) replay.commandResult().body().get("stateVersion")).longValue());
            assertEquals(((Number) first.commandResult().body().get("serverSeq")).longValue(),
                    ((Number) replay.commandResult().body().get("serverSeq")).longValue());
            assertEquals(before + 1, fixture.session.stateVersion(), "replay must not mutate the provider twice");
            assertEquals(before + 1, persistedVersion(fixture.db, "aoo_room_snapshot"));
            assertEquals(1, countWhere(fixture.db,
                    "SELECT COUNT(*) FROM aoo_business_idempotency WHERE status='COMPLETED'"));
        }
    }

    @Test
    void authorityRejectsWrongProviderBusinessIdentityWithoutDurableMutation() throws Exception {
        try (AuthorityFixture fixture = authorityFixture()) {
            long before = fixture.session.stateVersion();
            int currentSeat = currentSeat(fixture.session);
            long actor = actorAt(fixture.session, currentSeat);
            GameCommandRequest request = dispatch("poker.chengdu2001.dispatch", "authority-wrong-business-1",
                    before + 1, actor, currentSeat);

            assertThrows(SecurityException.class,
                    () -> fixture.authority.execute(command(identity(before), request)));

            assertEquals(before, fixture.session.stateVersion());
            assertEquals(before, persistedVersion(fixture.db, "aoo_room_runtime_identity_authority"));
            assertEquals(before, persistedVersion(fixture.db, "aoo_room_snapshot"));
            assertEquals(1, count(fixture.db, "aoo_business_idempotency"));
            assertEquals("UNKNOWN", scalarText(fixture.db,
                    "SELECT status FROM aoo_business_idempotency WHERE room_id=42"),
                    "the fail-closed attempt marker is not an authority snapshot mutation");
        }
    }

    private static GameProvider liangshanProvider() {
        return PokerCatalogRuntimeRegistry.providerFor(new GameDescriptor(
                2003, "liangshan2003", "凉山跑得快", GameCategory.POKER,
                "poker:pao-de-kuai", RegionScope.CITY, "sichuan", "liangshan",
                PLAY_VERSION)).orElseThrow();
    }

    private static AuthorityFixture authorityFixture() throws Exception {
        JdbcDataSource db = new JdbcDataSource();
        db.setURL("jdbc:h2:mem:provider_dispatch_" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        for (String statement : JdbcGatewayRoomAuthorityAtomicCommitTest.FIXTURE.split(";")) {
            if (!statement.isBlank()) JdbcGatewayRoomAuthorityAtomicCommitTest.sql(db, statement);
        }
        JdbcGatewayRoomAuthorityAtomicCommitTest.sql(db,
                "ALTER TABLE aoo_room_authority_route ADD updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        createBillingSchema(db);

        GameProvider provider = liangshanProvider();
        GameRoomHandle created = provider.roomFactory().create(new RoomCreationContext(
                ROOM_ID, 10L, Map.of("playerCount", 3, "roundCount", 8,
                "dealCardCount", 10, "robDealerRule", "dealer_last")));
        AuthoritativeGameSession session = created.requireAuthoritativeSession();
        session.execute(inner("join_req", "join-1", 1, 11L, 1, Map.of()));
        session.execute(inner("join_req", "join-2", 2, 12L, 2, Map.of()));
        session.execute(inner("ready_req", "ready-0", 3, 10L, 0, Map.of()));
        session.execute(inner("ready_req", "ready-1", 4, 11L, 1, Map.of()));
        session.execute(inner("ready_req", "ready-2", 5, 12L, 2, Map.of()));
        assertEquals("COMPETE_DEALER", session.viewFor(10L).get("phase"));
        long stateVersion = session.stateVersion();
        RoomRuntimeIdentity runtimeIdentity = runtimeIdentity(stateVersion);

        JdbcGatewayRoomAuthorityAtomicCommitTest.sql(db,
                "INSERT INTO aoo_room_authority_route(room_id,game_id,play_version,last_request_id,trace_id,node_id,lifecycle_state,lease_expires_at,fencing_token)"
                        + " VALUES(42,2003,'" + PLAY_VERSION + "','create-source-request-0001','trace-provider-dispatch','synthetic-node','ACTIVE',DATEADD('HOUR',1,CURRENT_TIMESTAMP),11)");
        JdbcGatewayRoomAuthorityAtomicCommitTest.sql(db,
                "INSERT INTO aoo_hall_room VALUES(42,2003,'" + PLAY_VERSION + "','LS',11,10,'{}','CLOSED')");
        JdbcGatewayRoomAuthorityAtomicCommitTest.sql(db,
                "INSERT INTO aoo_room_rule_lock(room_id,game_id,region_code,play_version,index_generation,release_id,rule_hash,component_hash,card_codec_version,event_interpreter_version,protocol_version,snapshot_schema_version,source_hash,immutable_rules)"
                        + " VALUES(42,2003,'LS','" + PLAY_VERSION + "',1,11,'" + RULE_HASH + "','"
                        + COMPONENT_HASH + "','codec-v1','events-v1','protocol-v1','snapshot-v1','"
                        + SOURCE_HASH + "','{}')");
        try (Connection connection = db.getConnection();
             var insert = connection.prepareStatement(
                     "INSERT INTO aoo_room_snapshot(room_id,game_id,play_version,release_id,component_version,rule_content_hash,component_content_hash,card_codec_version,event_interpreter_version,protocol_version,fencing_token,state_version,last_event_sequence,captured_at,state_payload) VALUES(42,2003,?,11,'test',?,?,?,?,?,11,?,?,CURRENT_TIMESTAMP,?)")) {
            insert.setString(1, PLAY_VERSION);
            insert.setString(2, RULE_HASH);
            insert.setString(3, COMPONENT_HASH);
            insert.setString(4, "codec-v1");
            insert.setString(5, "events-v1");
            insert.setString(6, "protocol-v1");
            insert.setLong(7, stateVersion);
            insert.setLong(8, stateVersion);
            insert.setString(9, JSON.writeValueAsString(session.authoritativeState()));
            insert.executeUpdate();
        }
        DataSource runtimeDb = dateSubCompatible(db);
        new RoomRuntimeIdentityStore(runtimeDb).create(ROOM_ID, runtimeIdentity);
        seedBilling(db, runtimeIdentity);

        RoomSnapshotMetadataSource metadata = new RoomSnapshotMetadataSource(11L, RULE_HASH,
                COMPONENT_HASH, "codec-v1", "events-v1", "protocol-v1", "snapshot-v1",
                1L, "LS", SOURCE_HASH);
        GameRoomHandle runtimeRoom = new GameRoomHandle(ROOM_ID, 2003, PLAY_VERSION, session,
                null, metadata, runtimeIdentity);
        RuntimeGameRoomRegistry rooms = new RuntimeGameRoomRegistry();
        rooms.bindRuntime(runtimeRoom);
        var games = GameRegistryFactory.production();
        games.register(provider);
        Clock clock = Clock.systemUTC();
        JdbcGatewayRoomAuthority authority = new JdbcGatewayRoomAuthority(runtimeDb, games, rooms, clock,
                "synthetic-node", "unit-test://no-network", JSON,
                JdbcGatewayRoomAuthorityAtomicCommitTest.committer(runtimeDb),
                new GatewayRoomBroadcastHub((roomId, accountId) -> {
                    throw new AssertionError("broadcast outside execute scope");
                }, JSON, clock),
                new HallRoomLifecycleClient(URI.create("unit-test://no-network"),
                        "synthetic-only-not-live-token-xxxxxxxx", JSON));
        authority.close();
        return new AuthorityFixture(db, authority, session);
    }

    private static void createBillingSchema(JdbcDataSource db) throws Exception {
        String ddl = """
                CREATE TABLE aoo_room_create_saga(request_id VARCHAR(128) PRIMARY KEY,request_hash CHAR(64),account_id BIGINT,room_id BIGINT);
                CREATE TABLE aoo_room_billing_intent_journal(intent_sequence BIGINT AUTO_INCREMENT UNIQUE,operation_key CHAR(64) PRIMARY KEY,command_hash CHAR(64),room_id BIGINT,room_attempt VARCHAR(48),billing_unit VARCHAR(48),actor_id BIGINT,payer_id BIGINT,game_id BIGINT,play_version VARCHAR(64),region_code VARCHAR(32),billing_kind VARCHAR(16),billing_context VARCHAR(16),payer_mode VARCHAR(16),measure_kind VARCHAR(32),measure_value INT,player_count INT,carry_minor BIGINT,created_at TIMESTAMP);
                CREATE TABLE aoo_room_billing_source_binding(source_namespace VARCHAR(32),source_request_id VARCHAR(128),source_request_hash CHAR(64),authenticated_actor_id BIGINT,operation_key CHAR(64),initiating_actor_id BIGINT,creator_id BIGINT,source_club_id BIGINT,subject_command_hash CHAR(64),release_id BIGINT,index_generation BIGINT,runtime_source_hash CHAR(64),classification_region_code VARCHAR(32),binding_hash CHAR(64),created_at TIMESTAMP,PRIMARY KEY(source_namespace,source_request_id),UNIQUE(operation_key));
                CREATE TABLE aoo_room_billing_business_receipt(source_namespace VARCHAR(32),source_request_id VARCHAR(128),event_type VARCHAR(32),operation_key CHAR(64),room_id BIGINT,creator_id BIGINT,room_instance_id VARCHAR(128),lifecycle_epoch BIGINT,fencing_token BIGINT,state_version BIGINT,game_id BIGINT,play_version VARCHAR(64),outcome_hash CHAR(64),created_at TIMESTAMP,PRIMARY KEY(source_namespace,source_request_id,event_type));
                CREATE TABLE replay_participant(room_id BIGINT,set_id INT,player_id BIGINT,seat_id INT,granted_at TIMESTAMP,PRIMARY KEY(room_id,set_id,player_id));
                CREATE TABLE perspective_replay_event(room_id BIGINT,set_id INT,event_sequence BIGINT,visibility VARCHAR(32),owner_player_id BIGINT,message_id VARCHAR(128),schema_version INT,play_version VARCHAR(64),release_id BIGINT,card_codec_version VARCHAR(64),event_interpreter_version VARCHAR(64),protocol_version VARCHAR(64),payload BLOB,content_hash CHAR(64) DEFAULT 'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',PRIMARY KEY(room_id,set_id,event_sequence,visibility,owner_player_id));
                CREATE TABLE replay_set_manifest(room_id BIGINT,set_id INT,play_version VARCHAR(64),schema_version INT,event_count BIGINT,content_hash CHAR(64),closed_at TIMESTAMP,archive_after TIMESTAMP,delete_after TIMESTAMP,legal_hold BOOLEAN,PRIMARY KEY(room_id,set_id));
                """;
        for (String statement : ddl.split(";")) {
            if (!statement.isBlank()) JdbcGatewayRoomAuthorityAtomicCommitTest.sql(db, statement);
        }
    }

    private static void seedBilling(JdbcDataSource db, RoomRuntimeIdentity identity) throws Exception {
        JdbcGatewayRoomAuthorityAtomicCommitTest.sql(db,
                "INSERT INTO aoo_room_create_saga VALUES('create-source-request-0001','" + RULE_HASH
                        + "',10,42)");
        try (Connection connection = db.getConnection()) {
            connection.setAutoCommit(false);
            var key = new RoomBillingSourceBinding.SourceKey("HALL_CREATE",
                    "create-source-request-0001", RULE_HASH, 10L);
            var operation = new RoomChargeOperation(ROOM_ID, "attempt", "CREATE", 10L, 10L,
                    2003, PLAY_VERSION, "LS", RoomChargeOperation.Kind.FIXED,
                    RoomChargeOperation.Context.HALL, RoomChargeOperation.Payer.OWNER,
                    "ROUND_COUNT", 8, 3, 0);
            var bound = new JdbcRoomBillingSourceBindingRepository(new JdbcRoomBillingIntentJournal())
                    .bind(connection, key,
                            new RoomBillingIntent(operation,
                                    Instant.now().truncatedTo(ChronoUnit.MILLIS)),
                            new JdbcRoomChargeCoordinator.Subject(10L, 10L, 0L),
                            new RoomBillingSourceBinding.AuthorityReference(11L, 1L, SOURCE_HASH, "LS"));
            new JdbcRoomBillingLifecyclePort(db,
                    () -> { throw new AssertionError("no fund operation expected"); }, Clock.systemUTC())
                    .snapshotCommitted(connection, bound, identity);
            connection.commit();
        }
    }

    private static RoomOrchestrator.ExecuteCommand command(RoomOrchestrator.RoomIdentity identity,
                                                            GameCommandRequest request) {
        return new RoomOrchestrator.ExecuteCommand(identity, "synthetic-session", request);
    }

    private static RoomOrchestrator.RoomIdentity identity(long stateVersion) {
        return new RoomOrchestrator.RoomIdentity(ROOM_ID, "synthetic-instance-42", 2003,
                "liangshan2003", PLAY_VERSION, 7, 11, stateVersion);
    }

    private static RoomRuntimeIdentity runtimeIdentity(long stateVersion) {
        return new RoomRuntimeIdentity("synthetic-instance-42", 7, 11, stateVersion);
    }

    private static int currentSeat(AuthoritativeGameSession session) {
        return ((Number) session.viewFor(10L).get("currentSeat")).intValue();
    }

    private static long actorAt(AuthoritativeGameSession session, int seat) {
        return ((Number) ((Map<?, ?>) session.authoritativeState().get("players")).get(seat)).longValue();
    }

    private static long persistedVersion(JdbcDataSource db, String table) throws Exception {
        try (Connection connection = db.getConnection();
             var query = connection.createStatement();
             var result = query.executeQuery("SELECT state_version FROM " + table + " WHERE room_id=42")) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private static long count(JdbcDataSource db, String table) throws Exception {
        try (Connection connection = db.getConnection();
             var query = connection.createStatement();
             var result = query.executeQuery("SELECT COUNT(*) FROM " + table)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private static long countWhere(JdbcDataSource db, String sql) throws Exception {
        try (Connection connection = db.getConnection();
             var query = connection.createStatement();
             var result = query.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getLong(1);
        }
    }

    private static DataSource dateSubCompatible(DataSource delegate) {
        return (DataSource) Proxy.newProxyInstance(DataSource.class.getClassLoader(),
                new Class<?>[]{DataSource.class}, (proxy, method, arguments) -> {
                    try {
                        Object result = method.invoke(delegate, arguments);
                        if (!"getConnection".equals(method.getName())) return result;
                        Connection connection = (Connection) result;
                        return Proxy.newProxyInstance(Connection.class.getClassLoader(),
                                new Class<?>[]{Connection.class}, (ignored, connectionMethod, connectionArguments) -> {
                                    Object[] rewritten = connectionArguments;
                                    if ("prepareStatement".equals(connectionMethod.getName())
                                            && connectionArguments != null && connectionArguments.length > 0
                                            && connectionArguments[0] instanceof String sql) {
                                        rewritten = connectionArguments.clone();
                                        rewritten[0] = sql.replace(
                                                "DATE_SUB(CURRENT_TIMESTAMP(3),INTERVAL 2 HOUR)",
                                                "DATEADD('HOUR',-2,CURRENT_TIMESTAMP(3))");
                                    }
                                    try {
                                        return connectionMethod.invoke(connection, rewritten);
                                    } catch (InvocationTargetException failure) {
                                        throw failure.getCause();
                                    }
                                });
                    } catch (InvocationTargetException failure) {
                        throw failure.getCause();
                    }
                });
    }

    private static String scalarText(JdbcDataSource db, String sql) throws Exception {
        try (Connection connection = db.getConnection();
             var query = connection.createStatement();
             var result = query.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getString(1);
        }
    }

    private record AuthorityFixture(JdbcDataSource db, JdbcGatewayRoomAuthority authority,
                                    AuthoritativeGameSession session) implements AutoCloseable {
        @Override public void close() throws Exception {
            authority.close();
            JdbcGatewayRoomAuthorityAtomicCommitTest.sql(db, "SHUTDOWN");
        }
    }

    private static GameCommandRequest dispatch(String msgId, String requestId, long sequence,
                                                long actor, int seat) {
        return new GameCommandRequest(msgId, requestId, sequence, ROOM_ID, 1, PLAY_VERSION,
                Long.toString(actor), seat,
                Map.of("action", "robDealer", "payload", Map.of("compete", true)));
    }

    private static GameCommandRequest inner(String msgId, String requestId, long sequence,
                                             long actor, int seat, Map<String, Object> body) {
        return new GameCommandRequest(msgId, requestId, sequence, ROOM_ID, 1, PLAY_VERSION,
                Long.toString(actor), seat, body);
    }
}
