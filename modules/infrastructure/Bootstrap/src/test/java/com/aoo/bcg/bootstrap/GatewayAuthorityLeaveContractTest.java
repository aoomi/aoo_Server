package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.RoomOrchestrator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class GatewayAuthorityLeaveContractTest {
    @Test
    void leaveIsTypedFencedIdempotentAndRoomOrchestratorOwned() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
        assertTrue(source.contains("leaveThroughRoomOrchestrator"));
        assertTrue(source.contains("roomOrchestrator.leave(toLeaveCommand(command))"));
        assertTrue(source.contains("new RoomOrchestrator.RoomIdentity(roomId"));
        assertTrue(source.contains("roomIdentityStore.require(roomId)"));
        assertTrue(source.contains("ROOM_ORCHESTRATOR_LEAVE_STATIC_IDENTITY_CONFLICT"));
        assertTrue(source.contains("ROOM_ORCHESTRATOR_LEAVE_IDENTITY_CONFLICT"));
        assertTrue(source.contains("ROOM_ORCHESTRATOR_LEAVE_COMMAND_CONFLICT"));
        assertTrue(source.contains("RoomOrchestrator.LeaveStatus.ALREADY_LEFT"));
        assertTrue(source.contains("requireLeaveIdentityFields(command)"));
        assertTrue(source.contains("leaveReceipts"));
        assertFalse(source.contains("return Map.of(\"status\", \"ALREADY_LEFT\")"));
    }

    @Test
    void leaveRejectsEveryMissingIdentityComponentBeforeDurableLookup() {
        Map<String, Object> complete = new HashMap<>();
        complete.put("roomInstanceId", "instance-1");
        complete.put("sessionId", "session-1");
        complete.put("lifecycleEpoch", 1L);
        complete.put("fencingToken", 1L);
        complete.put("stateVersion", 0L);
        complete.put("eventSequence", "0");
        complete.put("eventId", "leave-1");
        complete.put("requestId", "leave-1");
        complete.put("idempotencyKey", "leave-1");
        complete.put("gameId", 2003L);
        complete.put("gameCode", "liangshan2003");
        complete.put("permission", "SEATED_MEMBER");
        complete.put("releaseId", 11L);
        complete.put("indexGeneration", 1L);
        complete.put("regionCode", "LS");
        complete.put("sourceHash", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        complete.put("ruleContentHash", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        complete.put("componentContentHash", "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");
        complete.put("cardCodecVersion", "codec-v1");
        complete.put("eventInterpreterVersion", "events-v1");
        complete.put("protocolVersion", "protocol-v1");
        complete.put("snapshotSchemaVersion", "snapshot-v1");
        for (String missing : complete.keySet()) {
            // HTTP/Hall leave remains source-compatible without transport event
            // fields; the typed WebSocket route validates those separately.
            if (missing.equals("eventSequence") || missing.equals("eventId")
                    || missing.equals("sessionId") || missing.equals("permission")) {
                continue;
            }
            Map<String, Object> candidate = new HashMap<>(complete);
            candidate.remove(missing);
            assertThrows(IllegalArgumentException.class,
                    () -> JdbcGatewayRoomAuthority.requireLeaveIdentityFields(candidate), missing);
        }
        assertDoesNotThrow(() -> JdbcGatewayRoomAuthority.requireLeaveIdentityFields(complete));
    }

    @Test
    void httpActorUsesTheProductionJdbcSessionGate() throws Exception {
        var db = actorDatabase();
        try {
            var command = httpActor();
            assertDoesNotThrow(() -> JdbcGatewayRoomAuthority.requireHttpLeaveActor(db, command));
            for (String sid : new String[]{"missing", "revoked", "expired", "old-generation"}) {
                var invalid = new HashMap<>(command);
                invalid.put("accountSessionId", sid);
                assertThrows(SecurityException.class,
                        () -> JdbcGatewayRoomAuthority.requireHttpLeaveActor(db, invalid), sid);
            }
            var crossAccount = new HashMap<>(command);
            crossAccount.put("accountId", 18L);
            assertThrows(SecurityException.class,
                    () -> JdbcGatewayRoomAuthority.requireHttpLeaveActor(db, crossAccount));
            execute(db, "UPDATE aoo_account SET banned_until=DATEADD('HOUR',1,CURRENT_TIMESTAMP) WHERE account_id=17");
            assertThrows(SecurityException.class,
                    () -> JdbcGatewayRoomAuthority.requireHttpLeaveActor(db, command));
        } finally { shutdown(db); }
    }

    @Test
    void httpActorRejectsNamespacePermissionAndReservedSessionAliases() throws Exception {
        var db = actorDatabase();
        try {
            for (String field : new String[]{"actorType", "permission", "accountSessionId", "accountId"}) {
                var missing = new HashMap<>(httpActor());
                missing.remove(field);
                assertThrows(RuntimeException.class,
                        () -> JdbcGatewayRoomAuthority.requireHttpLeaveActor(db, missing), field);
            }
            for (String field : new String[]{"sessionId", "actorSessionId"}) {
                var mixed = new HashMap<>(httpActor());
                mixed.put(field, "room-connection");
                assertThrows(SecurityException.class,
                        () -> JdbcGatewayRoomAuthority.requireHttpLeaveActor(db, mixed), field);
            }
            var wrongPermission = new HashMap<>(httpActor());
            wrongPermission.put("permission", "SEATED_MEMBER");
            assertThrows(SecurityException.class,
                    () -> JdbcGatewayRoomAuthority.requireHttpLeaveActor(db, wrongPermission));
            var wrongNamespace = new HashMap<>(httpActor());
            wrongNamespace.put("actorType", "ROOM_CONNECTION");
            assertThrows(SecurityException.class,
                    () -> JdbcGatewayRoomAuthority.requireHttpLeaveActor(db, wrongNamespace));
        } finally { shutdown(db); }
    }

    @Test
    void httpIngressRejectsRevokedSessionBeforeAnExistingLeaveReceipt() throws Exception {
        var db = actorDatabase();
        JdbcGatewayRoomAuthority authority = null;
        try {
            authority = actorAuthority(db);
            // Stop only this in-process fixture's maintenance workers before invoking the real ingress.
            authority.close();
            var identity = new RoomOrchestrator.RoomIdentity(42, "instance-42", 2003,
                    "liangshan2003", "liangshan2003-v1", 7, 11, 3);
            var receipt = new RoomOrchestrator.LeaveResult(identity, 17L,
                    RoomOrchestrator.LeaveStatus.LEFT, 1, "leave-17");
            var field = JdbcGatewayRoomAuthority.class.getDeclaredField("leaveReceipts");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var receipts = (java.util.concurrent.ConcurrentHashMap<Long,
                    java.util.concurrent.ConcurrentHashMap<String, RoomOrchestrator.LeaveResult>>) field.get(authority);
            receipts.computeIfAbsent(42L, ignored -> new java.util.concurrent.ConcurrentHashMap<>())
                    .put("leave-17", receipt);
            execute(db, "UPDATE aoo_account_session SET revoked_at=CURRENT_TIMESTAMP WHERE session_id='active'");
            var command = new HashMap<>(httpActor());
            command.put("roomId", "42");
            command.put("requestId", "leave-17");
            // Room tables are intentionally removed: a canonical lookup before actor rejection would fail SQL.
            execute(db, "DROP TABLE aoo_room_authority_route");
            JdbcGatewayRoomAuthority actual = authority;
            var rejected = assertThrows(SecurityException.class,
                    () -> actual.leaveThroughRoomOrchestrator(command));
            assertTrue(rejected.getMessage().contains("ACTOR_SESSION_REJECTED"));
            assertTrue(receipts.get(42L).get("leave-17") == receipt);
        } finally { if (authority != null) authority.close(); shutdown(db); }
    }

    @Test
    void websocketLeaveKeepsConnectionAndEnvelopeBindingSeparateFromAccountSession() throws Exception {
        var session = new com.aoo.bcg.gateway.ConnectionSession("17", "42", 1,
                "liangshan2003-v1", 0L, "room-connection", 1L);
        var frame = new com.aoo.bcg.gateway.WebSocketFrame("2.0", "room.leave", "req",
                "leave-17", 1L, "trace-17", "42", 0, "liangshan2003-v1", 1L, Map.of());
        var method = JdbcGatewayRoomAuthority.class.getDeclaredMethod("requireTypedTransportIdentity",
                Map.class, com.aoo.bcg.gateway.ConnectionSession.class, com.aoo.bcg.gateway.WebSocketFrame.class);
        method.setAccessible(true);
        var wire = new HashMap<String,Object>();
        wire.put("accountId", "17"); wire.put("sessionId", "room-connection");
        wire.put("roomId", "42"); wire.put("playVersion", "liangshan2003-v1");
        wire.put("requestId", "leave-17"); wire.put("traceId", "trace-17");
        assertDoesNotThrow(() -> method.invoke(null, wire, session, frame));
        for (var change : Map.of("sessionId", "active", "accountId", "18",
                "roomId", "43", "playVersion", "wrong-version", "requestId", "other", "traceId", "other-trace").entrySet()) {
            var invalid = new HashMap<>(wire); invalid.put(change.getKey(), change.getValue());
            var error = assertThrows(java.lang.reflect.InvocationTargetException.class,
                    () -> method.invoke(null, invalid, session, frame), change.getKey());
            assertTrue(error.getCause() instanceof SecurityException);
        }
    }

    @Test
    void websocketLeaveRequiresItsOwnSessionAndSeatedPermission() {
        var wire = leaveIdentity();
        wire.put("sessionId", "room-connection"); wire.put("permission", "SEATED_MEMBER");
        assertDoesNotThrow(() -> JdbcGatewayRoomAuthority.requireWsLeaveIdentityFields(wire));
        for (String missing : new String[]{"sessionId", "permission"}) {
            var invalid = new HashMap<>(wire); invalid.remove(missing);
            assertThrows(RuntimeException.class,
                    () -> JdbcGatewayRoomAuthority.requireWsLeaveIdentityFields(invalid), missing);
        }
        for (String field : new String[]{"actorType", "accountSessionId", "actorSessionId"}) {
            var invalid = new HashMap<>(wire); invalid.put(field, "ACCOUNT_HTTP");
            assertThrows(SecurityException.class,
                    () -> JdbcGatewayRoomAuthority.requireWsLeaveIdentityFields(invalid), field);
        }
        wire.put("permission", "AUTHENTICATED_ROOM_MEMBER");
        assertThrows(IllegalArgumentException.class,
                () -> JdbcGatewayRoomAuthority.requireWsLeaveIdentityFields(wire));
    }

    @Test
    void actualLobbyProjectionJsonReachesTheProductionTypedLeaveParser() throws Exception {
        var db = actorDatabase();
        JdbcGatewayRoomAuthority authority = null;
        try {
            authority = actorAuthority(db);
            authority.close();
            seedTypedLeaveParser(db);
            var wire = lobbySerializedLeaveWire();
            assertTrue(wire.get("gameId") instanceof String);
            var typed = parseTypedLeave(authority, wire);
            org.junit.jupiter.api.Assertions.assertEquals(2003, typed.expectedIdentity().gameId());
            org.junit.jupiter.api.Assertions.assertEquals(9007199254740993001L, typed.expectedIdentity().lifecycleEpoch());
            org.junit.jupiter.api.Assertions.assertEquals(9007199254740993002L, typed.expectedIdentity().fencingToken());
            org.junit.jupiter.api.Assertions.assertEquals(9007199254740993003L, typed.expectedIdentity().stateVersion());
            org.junit.jupiter.api.Assertions.assertEquals(17L, typed.accountId());
            org.junit.jupiter.api.Assertions.assertEquals("leave-17", typed.commandId());
            var legacyInteger = new HashMap<>(wire); legacyInteger.put("gameId", 2003);
            org.junit.jupiter.api.Assertions.assertEquals(typed, parseTypedLeave(authority, legacyInteger));
        } finally { if (authority != null) authority.close(); shutdown(db); }
    }

    @Test
    void actualTypedParserRejectsMalformedOverflowAndMismatchedGameIdentity() throws Exception {
        var db = actorDatabase();
        JdbcGatewayRoomAuthority authority = null;
        try {
            authority = actorAuthority(db); authority.close(); seedTypedLeaveParser(db);
            var wire = lobbySerializedLeaveWire();
            for (Object invalid : new Object[]{"2004", "+2003", "02003", " 2003", "2003 ",
                    "2003.0", "2e3", "２００３", "0", "-1", "2147483648", "9223372036854775808",
                    2003.5d, 2003.0d, new java.math.BigDecimal("2003")}) {
                var candidate = new HashMap<>(wire); candidate.put("gameId", invalid);
                JdbcGatewayRoomAuthority actual = authority;
                var rejected = assertThrows(java.lang.reflect.InvocationTargetException.class,
                        () -> parseTypedLeave(actual, candidate), String.valueOf(invalid));
                assertTrue(rejected.getCause() instanceof RuntimeException);
            }
            for (var mismatch : Map.of("gameCode", "another-game", "playVersion", "another-version").entrySet()) {
                var candidate = new HashMap<>(wire); candidate.put(mismatch.getKey(), mismatch.getValue());
                JdbcGatewayRoomAuthority actual = authority;
                assertThrows(java.lang.reflect.InvocationTargetException.class,
                        () -> parseTypedLeave(actual, candidate), mismatch.getKey());
            }
        } finally { if (authority != null) authority.close(); shutdown(db); }
    }

    @Test
    void staticGateStillRejectsInstanceMismatchAgainstAKnownExpectedIdentity() throws Exception {
        var gate = JdbcGatewayRoomAuthority.class.getDeclaredMethod("requireLeaveStaticIdentity", Map.class,
                RoomOrchestrator.RoomIdentity.class); gate.setAccessible(true);
        var expected = new RoomOrchestrator.RoomIdentity(42,"instance-42",2003,"liangshan2003",
                "liangshan2003-v1.0.0",7,11,3);
        var candidate = new HashMap<String,Object>(Map.of("gameId","2003","roomInstanceId","another-instance"));
        var error = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> gate.invoke(null,candidate,expected));
        assertTrue(error.getCause() instanceof IllegalStateException);
        // The parser takes the supplied instance; current runtime freshness is not proved by this test.
    }

    private static Map<String,Object> lobbySerializedLeaveWire() throws Exception {
        Class<?> repo = com.aoo.bcg.lobby.JdbcLobbyRepository.class;
        var numbers = repo.getDeclaredMethod("losslessIdentityNumbers", Map.class); numbers.setAccessible(true);
        var metadata = new HashMap<String,Object>(leaveIdentity());
        metadata.put("gameId",2003L); metadata.put("releaseId",11L); metadata.put("indexGeneration",1L);
        metadata.put("playVersion","liangshan2003-v1.0.0");
        @SuppressWarnings("unchecked")
        var lossless = (Map<String,Object>) numbers.invoke(null,metadata);
        var runtime = Map.of("roomInstanceId","instance-42","lifecycleEpoch","9007199254740993001",
                "fencingToken","9007199254740993002","stateVersion","9007199254740993003");
        Class<?> room = java.util.Arrays.stream(repo.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals("Room")).findFirst().orElseThrow();
        var ctor = room.getDeclaredConstructors()[0]; ctor.setAccessible(true);
        var actualRoom = ctor.newInstance(2003L,"liangshan2003","LS","liangshan2003-v1.0.0","OPEN",
                "unit-test://no-network","PRIVATE",0L,"unit-template","unit-room");
        var admission = repo.getDeclaredMethod("leaveAdmission",long.class,room,Map.class); admission.setAccessible(true);
        @SuppressWarnings("unchecked")
        var body = (Map<String,Object>) admission.invoke(null,42L,actualRoom,
                Map.of("runtimeIdentity",runtime,"roomLockMetadata",lossless));
        var publicGate = repo.getDeclaredMethod("requirePublicLeaveIdentity",Map.class); publicGate.setAccessible(true);
        publicGate.invoke(null,body);
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        Map<String,Object> wire = json.readValue(json.writeValueAsBytes(body),
                new com.fasterxml.jackson.core.type.TypeReference<Map<String,Object>>() {});
        wire.putAll(httpActor()); wire.put("roomId",42L); wire.put("playVersion","liangshan2003-v1.0.0");
        wire.put("requestId","leave-17"); wire.put("idempotencyKey","leave-17"); wire.put("traceId","trace-17");
        return wire;
    }

    private static RoomOrchestrator.LeaveCommand parseTypedLeave(JdbcGatewayRoomAuthority authority,
                                                                Map<String,Object> wire) throws Exception {
        var parser = JdbcGatewayRoomAuthority.class.getDeclaredMethod("toLeaveCommand",Map.class);
        parser.setAccessible(true);
        return (RoomOrchestrator.LeaveCommand) parser.invoke(authority,wire);
    }

    private static void seedTypedLeaveParser(javax.sql.DataSource db) throws Exception {
        execute(db,"ALTER TABLE aoo_room_authority_route ADD created_at TIMESTAMP");
        execute(db,"ALTER TABLE aoo_room_authority_route ADD first_round_started_at TIMESTAMP");
        execute(db,"ALTER TABLE aoo_room_authority_route ADD last_business_activity_at TIMESTAMP");
        execute(db,"INSERT INTO aoo_room_authority_route VALUES(42,2003,'liangshan2003-v1.0.0','leave-17','trace-17','unit-node','ACTIVE',DATEADD('HOUR',1,CURRENT_TIMESTAMP),11,CURRENT_TIMESTAMP,NULL,CURRENT_TIMESTAMP)");
        execute(db,"CREATE TABLE aoo_room_rule_lock(room_id BIGINT,game_id INT,region_code VARCHAR(100),play_version VARCHAR(100),index_generation BIGINT,release_id BIGINT,rule_hash VARCHAR(64),component_hash VARCHAR(64),card_codec_version VARCHAR(100),event_interpreter_version VARCHAR(100),protocol_version VARCHAR(100),snapshot_schema_version VARCHAR(100),source_hash VARCHAR(64))");
        execute(db,"INSERT INTO aoo_room_rule_lock VALUES(42,2003,'LS','liangshan2003-v1.0.0',1,11,'"+"b".repeat(64)+"','"+"c".repeat(64)+"','codec-v1','events-v1','protocol-v1','snapshot-v1','"+"a".repeat(64)+"')");
    }

    private static Map<String,Object> httpActor() {
        return new HashMap<>(Map.of("actorType", "ACCOUNT_HTTP", "accountId", 17L,
                "accountSessionId", "active", "permission", "AUTHENTICATED_ROOM_MEMBER"));
    }

    private static Map<String,Object> leaveIdentity() {
        var identity = new HashMap<String,Object>();
        identity.put("roomInstanceId", "instance-1"); identity.put("lifecycleEpoch", "1");
        identity.put("fencingToken", "1"); identity.put("stateVersion", "0");
        identity.put("requestId", "leave-1"); identity.put("idempotencyKey", "leave-1");
        identity.put("gameId", "2003"); identity.put("gameCode", "liangshan2003");
        identity.put("releaseId", "11"); identity.put("indexGeneration", "1");
        identity.put("regionCode", "LS");
        identity.put("sourceHash", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        identity.put("ruleContentHash", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb");
        identity.put("componentContentHash", "cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc");
        identity.put("cardCodecVersion", "codec-v1"); identity.put("eventInterpreterVersion", "events-v1");
        identity.put("protocolVersion", "protocol-v1"); identity.put("snapshotSchemaVersion", "snapshot-v1");
        return identity;
    }

    private static org.h2.jdbcx.JdbcDataSource actorDatabase() throws Exception {
        var db = new org.h2.jdbcx.JdbcDataSource();
        db.setURL("jdbc:h2:mem:leave_actor_" + java.util.UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
        execute(db, "CREATE TABLE aoo_account(account_id BIGINT PRIMARY KEY,auth_generation BIGINT,banned_until TIMESTAMP)");
        execute(db, "CREATE TABLE aoo_account_session(session_id VARCHAR(100),account_id BIGINT,revoked_at TIMESTAMP,access_expires_at TIMESTAMP,auth_generation BIGINT)");
        execute(db, "INSERT INTO aoo_account VALUES(17,3,NULL),(18,3,NULL)");
        execute(db, "INSERT INTO aoo_account_session VALUES('active',17,NULL,DATEADD('HOUR',1,CURRENT_TIMESTAMP),3),('revoked',17,CURRENT_TIMESTAMP,DATEADD('HOUR',1,CURRENT_TIMESTAMP),3),('expired',17,NULL,DATEADD('HOUR',-1,CURRENT_TIMESTAMP),3),('old-generation',17,NULL,DATEADD('HOUR',1,CURRENT_TIMESTAMP),2)");
        execute(db, "CREATE TABLE aoo_room_authority_route(room_id BIGINT,game_id INT,play_version VARCHAR(100),last_request_id VARCHAR(100),trace_id VARCHAR(100),node_id VARCHAR(100),lifecycle_state VARCHAR(100),lease_expires_at TIMESTAMP,fencing_token BIGINT)");
        execute(db, "CREATE TABLE aoo_hall_room(room_id BIGINT,owner_account_id BIGINT,rules_json CLOB,state VARCHAR(100))");
        execute(db, "CREATE TABLE aoo_game_catalog(game_id INT,game_code VARCHAR(100))");
        return db;
    }

    private static JdbcGatewayRoomAuthority actorAuthority(javax.sql.DataSource db) {
        var clock = java.time.Clock.systemUTC();
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var games = com.aoo.bcg.gamespi.GameRegistryFactory.production();
        // Register the real catalog provider in this isolated parser fixture; no room factory or gameplay is invoked.
        games.register(new CatalogGameProvider(new com.aoo.bcg.gamespi.GameDescriptor(2003,"liangshan2003",
                "unit-parser-provider",com.aoo.bcg.gamespi.GameCategory.POKER,"poker",
                com.aoo.bcg.gamespi.RegionScope.CITY,"SC","LS","liangshan2003-v1.0.0"),true,"unit-fixture"));
        var settlements = new com.aoo.bcg.common.settlement.SettlementExecutor(
                new com.aoo.bcg.common.settlement.SettlementRepository() {
                    public com.aoo.bcg.common.settlement.SettlementResult save(String id,
                            com.aoo.bcg.common.settlement.SettlementResult value) {
                        throw new AssertionError("settlement is outside the HTTP actor test");
                    }
                    public java.util.Optional<com.aoo.bcg.common.settlement.SettlementResult> find(String id) {
                        throw new AssertionError("settlement is outside the HTTP actor test");
                    }
                });
        var committer = new JdbcGatewayGameCommandCommitter(db, json, clock, java.time.Duration.ofHours(24),
                new DurableGameSettlementService(games, settlements));
        var broadcasts = new com.aoo.bcg.gateway.GatewayRoomBroadcastHub((room, account) -> {
            throw new AssertionError("broadcast is outside the HTTP actor test");
        }, json, clock);
        return new JdbcGatewayRoomAuthority(db, games, new com.aoo.bcg.gateway.RuntimeGameRoomRegistry(),
                clock, "unit-node", "unit-test://no-network", json, committer, broadcasts,
                new HallRoomLifecycleClient(java.net.URI.create("unit-test://no-network"),
                        "unit-only-not-a-live-token-xxxxxxxx", json));
    }

    private static void execute(javax.sql.DataSource db, String sql) throws Exception {
        try (var c = db.getConnection(); var statement = c.createStatement()) { statement.execute(sql); }
    }

    private static void shutdown(javax.sql.DataSource db) throws Exception { execute(db, "SHUTDOWN"); }

    @Test
    void leaveSourceChecksCanonicalMetadataBeforeConstructingTypedCommand() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
        assertTrue(source.contains("requireLeaveCanonicalMetadata(command, canonical.source())"));
        assertTrue(source.contains("ROOM_ORCHESTRATOR_LEAVE_CANONICAL_METADATA_CONFLICT"));
        assertTrue(source.contains("ROOM_ORCHESTRATOR_LEAVE_IDEMPOTENCY_CONFLICT"));
        assertTrue(source.contains("ROOM_ORCHESTRATOR_LEAVE_PERMISSION_REJECTED"));
    }

    @Test
    void leaveImplementationHasNoGameplayMutationOrPublication() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
        int start = source.indexOf("public RoomOrchestrator.LeaveResult leave(");
        int end = source.indexOf("private static RoomOrchestrator.LeaveResult rememberLeave", start);
        String leave = source.substring(start, end);
        assertFalse(leave.contains("session.execute"));
        assertFalse(leave.contains("stateStore.saveSnapshot"));
        assertFalse(leave.contains("broadcasts.publishAuthoritativeState"));
        assertFalse(leave.contains("advanceIdentityIfRequired"));
        assertTrue(leave.contains("leaveReceipts"));
    }

    @Test
    void duplicateCommandWithDifferentMembershipCannotReplay() {
        RoomOrchestrator.RoomIdentity identity = new RoomOrchestrator.RoomIdentity(
                42, "instance-42", 2003, "liangshan2003", "liangshan2003-v1", 7, 11, 3);
        RoomOrchestrator.LeaveCommand command = new RoomOrchestrator.LeaveCommand(
                identity, 17L, "leave-17", "trace-17");
        RoomOrchestrator.LeaveResult conflicting = new RoomOrchestrator.LeaveResult(
                identity, 18L, RoomOrchestrator.LeaveStatus.LEFT, 1, "leave-17");
        assertThrows(IllegalStateException.class,
                () -> JdbcGatewayRoomAuthority.requireSameLeaveCommand(command, conflicting));
    }
}
