package com.aoo.bcg.bootstrap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aoo.bcg.gateway.WebSocketFrame;
import com.aoo.bcg.gamespi.RoomOrchestrator;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcGatewayRoomAuthorityTest {
    private static final long ROOM_ID = 742001;
    private static final int GAME_ID = 2003;
    private static final String PLAY_VERSION = "liangshan2003-v1.0.0";
    private static final String GAME_CODE = "liangshan2003";
    private static final String REGION = "CN-51-05";
    private static final String RULES = "{\"roundCount\":8,\"playerCount\":3,\"rule_ls201_0001\":[\"option_0001\"]}";
    private static final String RULE_VALIDATOR = "{\"gameId\":2003,\"gameCode\":\"liangshan2003\",\"playVersion\":\"liangshan2003-v1.0.0\",\"sourceHash\":\"" + "a".repeat(64) + "\"}";
    private static final String COMPONENT_CHAIN = "{\"components\":[\"component-a\"]}";
    private static final String INDEX_BUNDLE_HASH = sha256("{\"bundle\":\"v7\"}");
    private static final String RULE_HASH = sha256(RULE_VALIDATOR);
    private static final String COMPONENT_HASH = sha256(COMPONENT_CHAIN);

    private DataSource dataSource;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void createRoomSnapshotSchema() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:room-rule-snapshot-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource = source;
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE aoo_game_catalog (game_id INT PRIMARY KEY, game_code VARCHAR(64) NOT NULL)");
            statement.execute("CREATE TABLE aoo_compiled_room_create_index (game_id INT NOT NULL, region_code VARCHAR(32) NOT NULL, play_version VARCHAR(64) NOT NULL, index_generation BIGINT NOT NULL, release_id BIGINT NOT NULL, component_chain CLOB NOT NULL, rule_validator CLOB NOT NULL, bundle_hash VARCHAR(64) NOT NULL, lifecycle_state VARCHAR(16) NOT NULL, PRIMARY KEY(game_id,region_code,play_version,index_generation))");
            statement.execute("CREATE TABLE aoo_game_release (release_id BIGINT PRIMARY KEY, game_id INT NOT NULL, play_version VARCHAR(64) NOT NULL, status VARCHAR(16) NOT NULL, rule_hash VARCHAR(64) NOT NULL, component_hash VARCHAR(64) NOT NULL, bundle_hash VARCHAR(64) NOT NULL)");
            statement.execute("CREATE TABLE aoo_game_release_region (release_id BIGINT NOT NULL, region_code VARCHAR(32) NOT NULL, status VARCHAR(16) NOT NULL, PRIMARY KEY(release_id,region_code))");
            statement.execute("CREATE TABLE aoo_room_rule_lock (room_id BIGINT PRIMARY KEY, game_id INT NOT NULL, region_code VARCHAR(32) NOT NULL, play_version VARCHAR(64) NOT NULL, index_generation BIGINT NOT NULL, release_id BIGINT NOT NULL, component_chain CLOB NOT NULL, immutable_rules CLOB NOT NULL, bundle_hash VARCHAR(64) NOT NULL, rule_hash VARCHAR(64) NOT NULL, component_hash VARCHAR(64) NOT NULL)");
            statement.execute("CREATE TABLE aoo_hall_room (room_id BIGINT PRIMARY KEY, game_id INT NOT NULL, classification_region_code VARCHAR(32) NOT NULL, play_version VARCHAR(64) NOT NULL, release_id BIGINT NOT NULL, index_generation BIGINT NOT NULL, rules_json CLOB NOT NULL)");
            statement.execute("INSERT INTO aoo_game_catalog VALUES (2003,'liangshan2003')");
            statement.execute("INSERT INTO aoo_game_release VALUES (7001,2003,'" + PLAY_VERSION + "','ACTIVE','" + RULE_HASH + "','" + COMPONENT_HASH + "','" + INDEX_BUNDLE_HASH + "')");
            statement.execute("INSERT INTO aoo_game_release_region VALUES (7001,'" + REGION + "','ACTIVE')");
            // The room remains bound to generation 7 even after generation 8 is published.
            statement.execute("INSERT INTO aoo_compiled_room_create_index VALUES (2003,'" + REGION + "','" + PLAY_VERSION + "',7,7001,'" + COMPONENT_CHAIN + "','" + RULE_VALIDATOR + "','" + INDEX_BUNDLE_HASH + "','ACTIVE')");
            statement.execute("INSERT INTO aoo_compiled_room_create_index VALUES (2003,'" + REGION + "','" + PLAY_VERSION + "',8,8001,'" + COMPONENT_CHAIN + "','" + RULE_VALIDATOR + "','" + INDEX_BUNDLE_HASH + "','ACTIVE')");
            statement.execute("INSERT INTO aoo_hall_room VALUES (742001,2003,'" + REGION + "','" + PLAY_VERSION + "',7001,7,'" + RULES + "')");
            statement.execute("INSERT INTO aoo_room_rule_lock VALUES (742001,2003,'" + REGION + "','" + PLAY_VERSION + "',7,7001,'" + COMPONENT_CHAIN + "','" + RULES + "','" + INDEX_BUNDLE_HASH + "','" + RULE_HASH + "','" + COMPONENT_HASH + "')");
        }
    }

    @Test
    void runtimeRulesComeFromTheRoomPinnedReleaseRatherThanTheNewActiveGeneration() {
        Map<String, Object> rules = JdbcGatewayRoomAuthority.loadHallRoomRuleSnapshot(
                dataSource, json, ROOM_ID, GAME_ID, PLAY_VERSION, GAME_CODE);

        assertEquals(Map.of("roundCount", 8, "playerCount", 3, "rule_ls201_0001", java.util.List.of("option_0001")), rules);
    }

    @Test
    void readyTransportUsesCanonicalIdentityAndTypedAuthorityBridge() throws Exception {
        String authority = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
        String provider = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/ProductionGatewayRuntimeProvider.java"));
        assertTrue(authority.contains("readyGameplayThroughRoomOrchestrator"));
        assertTrue(authority.contains("new RoomOrchestrator.ReadyCommand"));
        assertTrue(authority.contains("requireCanonicalRoomLock(route)"));
        assertTrue(authority.contains("requireReadyPermission"));
        assertTrue(authority.contains("RoomState.WAITING"));
        assertTrue(authority.contains("roomOrchestrator.ready(command)"));
        assertTrue(provider.contains(
                "configureReadyAuthority(authority::readyGameplayThroughRoomOrchestrator)"));
    }

    @Test
    void handoffOwnerRegistrationIsMonotonicAndFailClosedForOldOrConflictingConnections() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/aoo/bcg/bootstrap/JdbcGatewayRoomAuthority.java"));
        int start = source.indexOf("void registerHandoffOwner(");
        int end = source.indexOf("/** Production Gateway transport bridge", start);
        assertTrue(start >= 0 && end > start, "handoff owner registration must remain bounded");
        String registration = source.substring(start, end);
        assertTrue(registration.contains("prior.sessionId().equals(replacement.sessionId())"));
        assertTrue(registration.contains("prior.connectionGeneration() == replacement.connectionGeneration()"));
        assertTrue(registration.contains("replacement.connectionGeneration() <= prior.connectionGeneration()"));
        assertTrue(registration.contains("ROOM_ORCHESTRATOR_HANDOFF_OWNER_REGISTRATION_CONFLICT"));
        assertTrue(registration.contains("return;"), "exact duplicate must be idempotent");
        assertTrue(registration.contains("handoffLeases.put(expectedIdentity.roomId(), replacement)"));
    }

    @Test
    void handoffOwnerLeaseAcceptsExactRetryAndNewerGenerationButRejectsOldOrConflictingTuple() {
        RoomOrchestrator.RoomIdentity identity = new RoomOrchestrator.RoomIdentity(
                ROOM_ID, "instance-742001", GAME_ID, GAME_CODE, PLAY_VERSION, 3, 8, 4);
        JdbcGatewayRoomAuthority.HandoffLease initial = new JdbcGatewayRoomAuthority.HandoffLease(
                identity, 17, "connection-1", 4);
        assertTrue(JdbcGatewayRoomAuthority.shouldReplaceHandoffLease(null, initial));
        assertTrue(!JdbcGatewayRoomAuthority.shouldReplaceHandoffLease(initial, initial));
        assertTrue(JdbcGatewayRoomAuthority.shouldReplaceHandoffLease(initial,
                new JdbcGatewayRoomAuthority.HandoffLease(identity, 17, "connection-2", 5)));
        IllegalStateException stale = assertThrows(IllegalStateException.class,
                () -> JdbcGatewayRoomAuthority.shouldReplaceHandoffLease(initial,
                        new JdbcGatewayRoomAuthority.HandoffLease(identity, 17, "connection-2", 3)));
        assertEquals("ROOM_ORCHESTRATOR_HANDOFF_OWNER_REGISTRATION_CONFLICT", stale.getMessage());
        IllegalStateException ownerConflict = assertThrows(IllegalStateException.class,
                () -> JdbcGatewayRoomAuthority.shouldReplaceHandoffLease(initial,
                        new JdbcGatewayRoomAuthority.HandoffLease(identity, 18, "connection-2", 5)));
        assertEquals("ROOM_ORCHESTRATOR_HANDOFF_OWNER_REGISTRATION_CONFLICT", ownerConflict.getMessage());
    }

    @Test
    void rejectsSnapshotWhenGameIdOrPlayVersionDoesNotMatchTheAuthorityRoute() {
        assertThrows(SecurityException.class, () -> JdbcGatewayRoomAuthority.loadHallRoomRuleSnapshot(
                dataSource, json, ROOM_ID, GAME_ID + 1, PLAY_VERSION, GAME_CODE));
        assertThrows(SecurityException.class, () -> JdbcGatewayRoomAuthority.loadHallRoomRuleSnapshot(
                dataSource, json, ROOM_ID, GAME_ID, "liangshan2003-v2.0.0", GAME_CODE));
    }

    @Test
    void rejectsCatalogGameCodeThatDoesNotMatchTheRuntimeProvider() {
        assertThrows(SecurityException.class, () -> JdbcGatewayRoomAuthority.loadHallRoomRuleSnapshot(
                dataSource, json, ROOM_ID, GAME_ID, PLAY_VERSION, "liangshan2002"));
    }

    @Test
    void refusesToCreateRuntimeWhenThePinnedRuleSnapshotCannotBeFound() {
        assertThrows(IllegalStateException.class, () -> JdbcGatewayRoomAuthority.loadHallRoomRuleSnapshot(
                dataSource, json, ROOM_ID + 1, GAME_ID, PLAY_VERSION, GAME_CODE));
    }

    @Test
    void rejectsPinnedRuleHashDrift() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE aoo_game_release SET rule_hash='" + "b".repeat(64) + "' WHERE release_id=7001");
        }
        IllegalStateException failure = assertThrows(IllegalStateException.class, () ->
                JdbcGatewayRoomAuthority.loadHallRoomRuleSnapshot(
                        dataSource, json, ROOM_ID, GAME_ID, PLAY_VERSION, GAME_CODE));
        assertEquals("ROOM_LOCK_RULE_HASH_CONFLICT", failure.getMessage());
    }

    @Test
    void rejectsPinnedComponentHashDrift() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE aoo_room_rule_lock SET component_hash='" + "c".repeat(64) + "' WHERE room_id=742001");
        }
        IllegalStateException failure = assertThrows(IllegalStateException.class, () ->
                JdbcGatewayRoomAuthority.loadHallRoomRuleSnapshot(
                        dataSource, json, ROOM_ID, GAME_ID, PLAY_VERSION, GAME_CODE));
        assertEquals("ROOM_LOCK_COMPONENT_HASH_CONFLICT", failure.getMessage());
    }

    @Test
    void rejectsPinnedBundleHashDrift() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("UPDATE aoo_compiled_room_create_index SET bundle_hash='" + "d".repeat(64) + "' WHERE release_id=7001");
        }
        IllegalStateException failure = assertThrows(IllegalStateException.class, () ->
                JdbcGatewayRoomAuthority.loadHallRoomRuleSnapshot(
                        dataSource, json, ROOM_ID, GAME_ID, PLAY_VERSION, GAME_CODE));
        assertEquals("ROOM_LOCK_BUNDLE_HASH_CONFLICT", failure.getMessage());
    }

    @Test
    void frozenLeaveRequestReadsCanonicalMetadataFromPayload() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("accountId", 17L);
        payload.put("roomInstanceId", "instance-742001");
        payload.put("sessionId", "session-17");
        payload.put("lifecycleEpoch", 3L);
        payload.put("fencingToken", 8L);
        payload.put("stateVersion", 4L);
        payload.put("eventSequence", "4");
        payload.put("eventId", "leave-17");
        payload.put("requestId", "leave-17");
        payload.put("idempotencyKey", "leave-17");
        payload.put("gameId", 2003L);
        payload.put("gameCode", GAME_CODE);
        payload.put("permission", "SEATED_MEMBER");
        payload.put("releaseId", 7001L);
        payload.put("indexGeneration", 7L);
        payload.put("regionCode", REGION);
        payload.put("sourceHash", "a".repeat(64));
        payload.put("ruleContentHash", RULE_HASH);
        payload.put("componentContentHash", COMPONENT_HASH);
        payload.put("cardCodecVersion", "card-v1");
        payload.put("eventInterpreterVersion", "event-v1");
        payload.put("protocolVersion", "protocol-v2");
        payload.put("snapshotSchemaVersion", "snapshot-v1");

        WebSocketFrame frame = new WebSocketFrame("room.leave", "leave-17", 1,
                Long.toString(ROOM_ID), 0, PLAY_VERSION, 1L,
                Map.of("action", "room.leave", "payload", payload));
        Map<String, Object> command = JdbcGatewayRoomAuthority.typedCommandBody(frame);

        assertEquals("room.leave", command.get("action"));
        assertEquals("instance-742001", command.get("roomInstanceId"));
        assertEquals(Long.toString(ROOM_ID), command.get("roomId"));
        assertEquals(PLAY_VERSION, command.get("playVersion"));
        assertEquals("leave-17", command.get("requestId"));
        assertDoesNotThrow(() -> JdbcGatewayRoomAuthority.requireLeaveIdentityFields(command));
    }

    @Test
    void frozenLeaveRequestRejectsMissingPayloadMetadataAndTopLevelIdentity() {
        Map<String, Object> missing = Map.of("action", "room.leave", "payload",
                Map.of("accountId", 17L, "roomInstanceId", "instance-742001"));
        WebSocketFrame missingFrame = new WebSocketFrame("room.leave", "leave-missing", 1,
                Long.toString(ROOM_ID), 0, PLAY_VERSION, 1L, missing);
        Map<String, Object> missingCommand = JdbcGatewayRoomAuthority.typedCommandBody(missingFrame);
        assertThrows(IllegalArgumentException.class,
                () -> JdbcGatewayRoomAuthority.requireLeaveIdentityFields(missingCommand));

        Map<String, Object> topLevel = new LinkedHashMap<>();
        topLevel.put("action", "room.leave");
        topLevel.put("payload", Map.of());
        topLevel.put("roomInstanceId", "instance-742001");
        WebSocketFrame topLevelFrame = new WebSocketFrame("room.leave", "leave-top-level", 1,
                Long.toString(ROOM_ID), 0, PLAY_VERSION, 1L, topLevel);
        assertThrows(SecurityException.class,
                () -> JdbcGatewayRoomAuthority.typedCommandBody(topLevelFrame));
    }

    @Test
    void frozenDissolveApplyRequiresTopLevelReasonCode() {
        Map<String, Object> payloadOnly = Map.of("reasonCode", "PLAYER_REQUEST");
        WebSocketFrame missing = new WebSocketFrame("room.dissolve_apply", "apply-missing", 1,
                Long.toString(ROOM_ID), 0, PLAY_VERSION, 1L,
                Map.of("action", "room.dissolve_apply", "payload", payloadOnly));
        assertThrows(IllegalArgumentException.class,
                () -> JdbcGatewayRoomAuthority.typedCommandBody(missing));

        Map<String, Object> conflictPayload = Map.of("reasonCode", "PAYLOAD_REASON");
        WebSocketFrame conflict = new WebSocketFrame("room.dissolve_apply", "apply-conflict", 1,
                Long.toString(ROOM_ID), 0, PLAY_VERSION, 1L,
                Map.of("action", "room.dissolve_apply", "payload", conflictPayload,
                        "reasonCode", "TOP_LEVEL_REASON"));
        assertThrows(SecurityException.class,
                () -> JdbcGatewayRoomAuthority.typedCommandBody(conflict));

        WebSocketFrame valid = new WebSocketFrame("room.dissolve_apply", "apply-valid", 1,
                Long.toString(ROOM_ID), 0, PLAY_VERSION, 1L,
                Map.of("action", "room.dissolve_apply", "payload", Map.of(),
                        "reasonCode", "PLAYER_REQUEST"));
        assertEquals("PLAYER_REQUEST",
                JdbcGatewayRoomAuthority.typedCommandBody(valid).get("reasonCode"));
    }

    @Test
    void frozenDissolveVoteRequiresTopLevelVoteIdAndAgree() {
        Map<String, Object> payloadOnly = Map.of("voteId", "vote-payload", "agree", Boolean.TRUE);
        WebSocketFrame missing = new WebSocketFrame("room.dissolve_vote", "vote-missing", 1,
                Long.toString(ROOM_ID), 0, PLAY_VERSION, 1L,
                Map.of("action", "room.dissolve_vote", "payload", payloadOnly));
        assertThrows(IllegalArgumentException.class,
                () -> JdbcGatewayRoomAuthority.typedCommandBody(missing));

        WebSocketFrame missingAgree = new WebSocketFrame("room.dissolve_vote", "vote-agree-missing", 1,
                Long.toString(ROOM_ID), 0, PLAY_VERSION, 1L,
                Map.of("action", "room.dissolve_vote", "payload", Map.of(),
                        "voteId", "vote-1"));
        assertThrows(IllegalArgumentException.class,
                () -> JdbcGatewayRoomAuthority.typedCommandBody(missingAgree));

        WebSocketFrame conflict = new WebSocketFrame("room.dissolve_vote", "vote-conflict", 1,
                Long.toString(ROOM_ID), 0, PLAY_VERSION, 1L,
                Map.of("action", "room.dissolve_vote",
                        "payload", Map.of("voteId", "payload-vote", "agree", Boolean.FALSE),
                        "voteId", "top-vote", "agree", Boolean.TRUE));
        assertThrows(SecurityException.class,
                () -> JdbcGatewayRoomAuthority.typedCommandBody(conflict));

        WebSocketFrame valid = new WebSocketFrame("room.dissolve_vote", "vote-valid", 1,
                Long.toString(ROOM_ID), 0, PLAY_VERSION, 1L,
                Map.of("action", "room.dissolve_vote", "payload", Map.of(),
                        "voteId", "vote-1", "agree", Boolean.TRUE));
        Map<String, Object> command = JdbcGatewayRoomAuthority.typedCommandBody(valid);
        assertEquals("vote-1", command.get("voteId"));
        assertEquals(Boolean.TRUE, command.get("agree"));
    }

    @Test
    void leaveResponseContainsFrozenLeftBooleanForIdempotentStatuses() {
        RoomOrchestrator.RoomIdentity identity = new RoomOrchestrator.RoomIdentity(
                ROOM_ID, "instance-742001", GAME_ID, GAME_CODE, PLAY_VERSION, 3, 8, 4);
        RoomOrchestrator.LeaveResult left = new RoomOrchestrator.LeaveResult(
                identity, 17, RoomOrchestrator.LeaveStatus.LEFT, 2, "leave-17");
        RoomOrchestrator.LeaveResult alreadyLeft = new RoomOrchestrator.LeaveResult(
                identity, 17, RoomOrchestrator.LeaveStatus.ALREADY_LEFT, 2, "leave-17");

        assertEquals(Boolean.TRUE, JdbcGatewayRoomAuthority.leaveResponse(left).get("left"));
        assertEquals(Boolean.TRUE, JdbcGatewayRoomAuthority.leaveResponse(alreadyLeft).get("left"));
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
    }
}
