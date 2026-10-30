package com.aoo.bcg.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** SQL is a fixed SELECT allowlist and every connection is explicitly read-only. */
public final class JdbcGameInvestigationRepository implements GameInvestigationService.ReadOnlyRepository {
    private static final TypeReference<Map<String, Object>> MAP = new TypeReference<>() { };
    private final JdbcAdminResourceRepository.ConnectionFactory connections;
    private final ObjectMapper json;

    public JdbcGameInvestigationRepository(JdbcAdminResourceRepository.ConnectionFactory connections, ObjectMapper json) {
        this.connections = Objects.requireNonNull(connections);
        this.json = Objects.requireNonNull(json);
    }

    @Override public GameInvestigationService.Snapshot snapshot(long roomId) {
        String sql = "SELECT game_id,play_version,fencing_token,last_event_sequence,captured_at,state_payload "
                + "FROM aoo_room_snapshot WHERE room_id=?";
        try (Connection connection = readOnly(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId);
            try (ResultSet row = statement.executeQuery()) {
                if (!row.next()) throw new IllegalArgumentException("room snapshot not found");
                return new GameInvestigationService.Snapshot(roomId, row.getLong(1), row.getString(2),
                        row.getLong(3), row.getLong(4), row.getTimestamp(5).toInstant(),
                        json.readValue(row.getString(6), MAP));
            }
        } catch (IllegalArgumentException error) { throw error; }
        catch (Exception error) { throw new IllegalStateException("cannot read room snapshot", error); }
    }

    @Override public List<GameInvestigationService.Event> events(long roomId, long from, long to) {
        String sql = "SELECT event_sequence,event_type,event_payload,created_at FROM aoo_room_event "
                + "WHERE room_id=? AND event_sequence BETWEEN ? AND ? ORDER BY event_sequence LIMIT 10001";
        try (Connection connection = readOnly(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId); statement.setLong(2, from); statement.setLong(3, to);
            List<GameInvestigationService.Event> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(new GameInvestigationService.Event(rows.getLong(1), rows.getString(2),
                        json.readValue(rows.getString(3), MAP), rows.getTimestamp(4).toInstant()));
            }
            return List.copyOf(result);
        } catch (Exception error) { throw new IllegalStateException("cannot read room events", error); }
    }

    @Override public List<GameInvestigationService.ReplayFrame> replay(long roomId, int setId) {
        String sql = "SELECT event_sequence,visibility,owner_player_id,message_id,schema_version,play_version,payload "
                + "FROM perspective_replay_event WHERE room_id=? AND set_id=? ORDER BY event_sequence LIMIT 10001";
        try (Connection connection = readOnly(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId); statement.setInt(2, setId);
            List<GameInvestigationService.ReplayFrame> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(new GameInvestigationService.ReplayFrame(rows.getLong(1),
                        rows.getString(2), rows.getLong(3), rows.getString(4), rows.getInt(5),
                        rows.getString(6), rows.getBytes(7)));
            }
            return List.copyOf(result);
        } catch (Exception error) { throw new IllegalStateException("cannot read replay evidence", error); }
    }

    private Connection readOnly() throws Exception {
        Connection connection = connections.open();
        try { connection.setReadOnly(true); return connection; }
        catch (Exception error) { connection.close(); throw error; }
    }
}
