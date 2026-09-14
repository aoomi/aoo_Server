package core.server;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.player.Player;
import com.aoo.bcg.gateway.RuntimeGameRoomRegistry;
import com.ddm.server.protocol.v2.ProtocolV2AuthorityRuntime;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** JDBC-backed player-perspective snapshot and incremental recovery authority. */
final class ProductionRoomReconnectService {
    private static final int PAGE_SIZE = 500;
    private static final Set<String> SECRET_FIELDS = Set.of("password", "passwordDES", "gps", "location",
            "longitude", "latitude", "hand", "cards", "drawCard", "deck", "wall");
    private final DataSource dataSource;
    private final ObjectMapper mapper;

    ProductionRoomReconnectService(DataSource dataSource, ObjectMapper mapper) {
        this.dataSource = dataSource;
        this.mapper = mapper;
    }

    Map<String,Object> reconnect(Player player, ProtocolV2AuthorityRuntime.Command command) {
        long roomId = positiveLong(command.roomId(), "roomId");
        Map<String,Object> body = command.body();
        long lastSeq = nonNegativeLong(body.get("lastServerSeq"), "lastServerSeq");
        long expectedStateVersion = nonNegativeLong(body.get("expectedStateVersion"), "expectedStateVersion");
        requireReconnectToken(player.getUUID(), body.get("reconnectToken"));

        Membership membership = membership(player, roomId);
        SnapshotHead head = snapshotHead(roomId);
        if (!head.playVersion().equals(command.playVersion())) throw rejected("ROOM_PLAY_VERSION_CONFLICT");
        if (lastSeq > head.lastEventSequence()) throw rejected("ROOM_RECOVERY_CURSOR_AHEAD");
        if (expectedStateVersion > head.stateVersion()) throw rejected("ROOM_STATE_VERSION_CONFLICT");

        List<Map<String,Object>> events = readEvents(roomId, player.getPid(), lastSeq,
                membership.player(), PAGE_SIZE + 1);
        boolean hasMore = events.size() > PAGE_SIZE;
        if (hasMore) events = new ArrayList<>(events.subList(0, PAGE_SIZE));
        long responseSeq = hasMore ? number(events.get(events.size() - 1).get("sequence"))
                : head.lastEventSequence();
        return Map.of("viewerSnapshot", membership.viewerSnapshot(), "events", List.copyOf(events),
                "serverSeq", responseSeq, "stateVersion", head.stateVersion(),
                "playVersion", head.playVersion(), "hasMore", hasMore);
    }

    private Membership membership(Player player, long roomId) {
        AbsBaseRoom room = RoomMgr.getInstance().getRoom(roomId);
        if (room != null) {
            AbsRoomPos seated = room.getRoomPosMgr().getPosByPid(player.getPid());
            boolean watcher = room.getRoomPosMgr().getWatchPosByPid(player.getPid()) != null;
            if (seated == null && !watcher) throw new SecurityException("ROOM_VIEWER_NOT_AUTHORIZED");
            Object raw = mapper.convertValue(room.getRoomInfo(player.getPid()), Object.class);
            return new Membership(seated != null, crop(raw, seated != null));
        }
        var authority = RuntimeGameRoomRegistry.global().require(roomId).requireAuthoritativeSession();
        Object playersRaw = authority.authoritativeState().get("players");
        boolean seated = playersRaw instanceof Map<?,?> players && players.values().stream()
                .anyMatch(value -> positiveLong(value, "playerId") == player.getPid());
        if (!seated) throw new SecurityException("ROOM_VIEWER_NOT_AUTHORIZED");
        return new Membership(true, crop(authority.viewFor(player.getPid()), true));
    }

    private SnapshotHead snapshotHead(long roomId) {
        String sql = "SELECT play_version,state_version,last_event_sequence FROM aoo_room_snapshot WHERE room_id=?";
        try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId);
            try (var row = statement.executeQuery()) {
                if (!row.next()) throw rejected("ROOM_SNAPSHOT_NOT_FOUND");
                return new SnapshotHead(row.getString(1), row.getLong(2), row.getLong(3));
            }
        } catch (RuntimeException failure) {
            throw failure;
        } catch (Exception failure) {
            throw new IllegalStateException("ROOM_SNAPSHOT_READ_FAILED", failure);
        }
    }

    private List<Map<String,Object>> readEvents(long roomId, long playerId, long cursor,
            boolean player, int limit) {
        String visibility = player
                ? "((visibility='PUBLIC' AND owner_player_id=0) OR (visibility='PLAYER_PRIVATE' AND owner_player_id=?))"
                : "(visibility='PUBLIC' AND owner_player_id=0)";
        String sql = "SELECT event_sequence,event_type,event_payload FROM aoo_room_event WHERE room_id=? "
                + "AND event_sequence>? AND " + visibility
                + " ORDER BY event_sequence,visibility,owner_player_id LIMIT ?";
        try (Connection connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId);
            statement.setLong(2, cursor);
            int index = 3;
            if (player) statement.setLong(index++, playerId);
            statement.setInt(index, limit);
            List<Map<String,Object>> result = new ArrayList<>();
            try (var rows = statement.executeQuery()) {
                while (rows.next()) {
                    Object payload = mapper.readValue(rows.getString(3), new TypeReference<Map<String,Object>>() { });
                    result.add(Map.of("sequence", rows.getLong(1), "messageId", rows.getString(2),
                            "payload", crop(payload, player)));
                }
            }
            return result;
        } catch (Exception failure) {
            throw new IllegalStateException("ROOM_RECOVERY_EVENT_READ_FAILED", failure);
        }
    }

    private Object crop(Object value, boolean playerView) {
        if (value instanceof Map<?,?> source) {
            Map<String,Object> clean = new LinkedHashMap<>();
            for (var entry : source.entrySet()) {
                String key = String.valueOf(entry.getKey());
                if (SECRET_FIELDS.contains(key) && !playerView) continue;
                if (Set.of("password", "passwordDES", "gps", "location", "longitude", "latitude").contains(key)) continue;
                clean.put(key, crop(entry.getValue(), playerView));
            }
            return Map.copyOf(clean);
        }
        if (value instanceof List<?> list) return list.stream().map(item -> crop(item, playerView)).toList();
        return value;
    }

    private static void requireReconnectToken(String expected, Object supplied) {
        if (expected == null || expected.isBlank() || !(supplied instanceof String token) || token.isBlank()
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8)))
            throw new SecurityException("ROOM_RECONNECT_TOKEN_INVALID");
    }
    private static long positiveLong(Object value, String field) {
        long parsed = number(value);
        if (parsed <= 0) throw new IllegalArgumentException("ROOM_INVALID_" + field.toUpperCase());
        return parsed;
    }
    private static long nonNegativeLong(Object value, String field) {
        long parsed = number(value);
        if (parsed < 0) throw new IllegalArgumentException("ROOM_INVALID_" + field.toUpperCase());
        return parsed;
    }
    private static long number(Object value) {
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(String.valueOf(value)); }
        catch (RuntimeException failure) { throw new IllegalArgumentException("ROOM_INVALID_NUMBER"); }
    }
    private static IllegalArgumentException rejected(String code) { return new IllegalArgumentException(code); }
    private record Membership(boolean player, Object viewerSnapshot) { }
    private record SnapshotHead(String playVersion, long stateVersion, long lastEventSequence) { }
}
