package com.aoo.bcg.hall.room;

import com.aoo.bcg.hall.http.HallError;
import com.aoo.bcg.lobby.JdbcLobbyRepository;
import com.aoo.bcg.lobby.LobbyMembershipAuthorityPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Read-only durable runtime projections are owned by Lobby and exposed by Hall. */
final class RoomRuntimeIdentityProjectionContractTest {
    @Test
    void roomAndConfirmationProjectDurableIdentityAndRoomLockMetadata() {
        ProjectionFixture fixture = new ProjectionFixture(true, true, false);

        Map<String, Object> canonical = fixture.lobby.room(7001L, 901001L);
        Map<String, Object> compatibility = fixture.hall.room(7001L, 901001L);

        assertEquals(canonical, compatibility);
        assertEquals("instance-901001", ((Map<?, ?>) canonical.get("runtimeIdentity")).get("roomInstanceId"));
        assertEquals("a".repeat(64), ((Map<?, ?>) canonical.get("roomLockMetadata")).get("sourceHash"));
        assertEquals("codec-v1", ((Map<?, ?>) canonical.get("roomLockMetadata")).get("cardCodecVersion"));
        assertEquals("9007199254740993", ((Map<?, ?>) canonical.get("runtimeIdentity")).get("lifecycleEpoch"));
        assertEquals(Long.toString(Long.MAX_VALUE), ((Map<?, ?>) canonical.get("runtimeIdentity")).get("fencingToken"));
        Map<?,?> admission=(Map<?,?>)canonical.get("leaveAdmission");
        assertEquals(18,admission.size());assertEquals("901001",admission.get("roomId"));
        assertEquals("11",admission.get("releaseId"));assertEquals("3",admission.get("indexGeneration"));
        try { Map<?,?> roundtrip=new ObjectMapper().readValue(new ObjectMapper().writeValueAsBytes(admission),Map.class);
            assertEquals("9007199254740993",roundtrip.get("lifecycleEpoch"));
        } catch(Exception e) { throw new AssertionError(e); }
    }

    @Test
    void projectionsRejectMissingOrInvalidRowsWithoutDefaults() {
        HallError missingIdentity = assertThrows(HallError.class,
                () -> new ProjectionFixture(false, true, false).lobby.room(7001L, 901001L));
        assertEquals("HALL_RUNTIME_IDENTITY_UNAVAILABLE", missingIdentity.code());

        HallError invalidIdentity = assertThrows(HallError.class,
                () -> new ProjectionFixture(true, true, true).lobby.room(7001L, 901001L));
        assertEquals("HALL_RUNTIME_IDENTITY_CONFLICT", invalidIdentity.code());

        HallError missingLock = assertThrows(HallError.class,
                () -> new ProjectionFixture(true, false, false).lobby.room(7001L, 901001L));
        assertEquals("HALL_ROOM_RULE_LOCK_METADATA_UNAVAILABLE", missingLock.code());
    }

    private static final class ProjectionFixture {
        final JdbcLobbyRepository lobby;
        final JdbcHallRepository hall;

        ProjectionFixture(boolean identity, boolean lock, boolean invalidIdentity) {
            DataSource source = fake(DataSource.class,
                    (proxy, method, args) -> "getConnection".equals(method.getName())
                            ? connection(identity, lock, invalidIdentity) : null);
            LobbyMembershipAuthorityPort authority = fake(LobbyMembershipAuthorityPort.class,
                    (proxy, method, args) -> null);
            lobby = new JdbcLobbyRepository(source, new ObjectMapper(), Clock.systemUTC(), authority);
            hall = new JdbcHallRepository(source, new ObjectMapper(), Clock.systemUTC(),
                    fake(RoomMembershipAuthorityPort.class, (proxy, method, args) -> null));
        }

        private static Connection connection(boolean identity, boolean lock, boolean invalidIdentity) {
            return fake(Connection.class, (proxy, method, args) -> {
                if ("prepareStatement".equals(method.getName())) {
                    return statement(String.valueOf(args[0]), identity, lock, invalidIdentity);
                }
                return null;
            });
        }

        private static PreparedStatement statement(String sql, boolean identity, boolean lock,
                boolean invalidIdentity) {
            return fake(PreparedStatement.class, (proxy, method, args) -> {
                if ("executeQuery".equals(method.getName())) {
                    if (sql.startsWith("SELECT h.game_id,g.game_code")) {
                        return rows(List.of(Map.ofEntries(Map.entry("game_id", 101L),
                                Map.entry("game_code", "CD101"), Map.entry("classification_region_code", "CD"),
                                Map.entry("play_version", "v1"), Map.entry("state", "OPEN"),
                                Map.entry("route_endpoint", "hall://cd101"), Map.entry("scope_type", "PERSONAL"),
                                Map.entry("club_id", 0L), Map.entry("club_template_code", ""),
                                Map.entry("display_name", "room"))));
                    }
                    if (sql.startsWith("SELECT 1 FROM aoo_hall_room_member")) return rows(List.of(Map.of("1", 1)));
                    if (sql.startsWith("SELECT account_id,seat_no")) return rows(List.of());
                    if (sql.startsWith("SELECT rules_json")) return rows(List.of(Map.of("1", "{}")));
                    if (sql.startsWith("SELECT h.game_id,h.classification_region_code")) {
                        return rows(List.of(Map.ofEntries(Map.entry("game_id", 101L),
                                Map.entry("classification_region_code", "CD"), Map.entry("play_version", "v1"),
                                Map.entry("release_id", 11L), Map.entry("index_generation", 3L),
                                Map.entry("component_chain", "{}"), Map.entry("rule_validator", "{}"),
                                Map.entry("ui_schema", "{\"bundle\":\"bundle\",\"scene\":\"Room\"}"),
                                Map.entry("bundle_hash", "bundle-hash"), Map.entry("family_code", "poker"))));
                    }
                    if (sql.startsWith("SELECT room_instance_id")) {
                        if (!identity) return rows(List.of());
                        return rows(List.of(Map.of("room_instance_id", invalidIdentity ? "" : "instance-901001",
                                "lifecycle_epoch", 9007199254740993L, "fencing_token", Long.MAX_VALUE, "state_version", 3L,
                                "lifecycle_state", "ACTIVE")));
                    }
                    if (sql.startsWith("SELECT game_id,region_code")) {
                        if (!lock) return rows(List.of());
                        return rows(List.of(Map.ofEntries(Map.entry("game_id", 101L), Map.entry("region_code", "CD"),
                                Map.entry("play_version", "v1"), Map.entry("release_id", 11L),
                                Map.entry("index_generation", 3L), Map.entry("source_hash", "a".repeat(64)),
                                Map.entry("rule_hash", "b".repeat(64)), Map.entry("component_hash", "c".repeat(64)),
                                Map.entry("card_codec_version", "codec-v1"),
                                Map.entry("event_interpreter_version", "events-v1"),
                                Map.entry("protocol_version", "protocol-v1"),
                                Map.entry("snapshot_schema_version", "snapshot-v1"))));
                    }
                    if (sql.startsWith("SELECT i.ui_schema")) {
                        return rows(List.of(Map.of("1", "{\"bundle\":\"bundle\",\"scene\":\"Room\"}")));
                    }
                    return rows(List.of());
                }
                return null;
            });
        }
    }

    private static ResultSet rows(List<Map<String, Object>> values) {
        List<Map<String, Object>> rows = new ArrayList<>(values);
        return fake(ResultSet.class, new java.lang.reflect.InvocationHandler() {
            int index = -1;
            @Override public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) {
                if ("next".equals(method.getName())) return ++index < rows.size();
                if ("wasNull".equals(method.getName())) return false;
                if (method.getName().startsWith("get") && args != null && args.length > 0) {
                    Object value = rows.get(index).get(String.valueOf(args[0]));
                    return switch (method.getName()) {
                        case "getLong" -> value == null ? 0L : ((Number) value).longValue();
                        case "getInt" -> value == null ? 0 : ((Number) value).intValue();
                        case "getString" -> value == null ? null : String.valueOf(value);
                        default -> value;
                    };
                }
                return null;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T fake(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }
}
