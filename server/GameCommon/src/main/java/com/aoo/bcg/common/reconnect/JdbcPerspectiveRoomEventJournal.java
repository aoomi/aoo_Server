package com.aoo.bcg.common.reconnect;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JdbcPerspectiveRoomEventJournal implements PerspectiveRoomEventJournal {
    private static final int MAX_PAGE_SIZE = 500;
    private final DataSource dataSource;
    private final ObjectMapper mapper;

    public JdbcPerspectiveRoomEventJournal(DataSource dataSource, ObjectMapper mapper) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    @Override
    public void appendPublic(long roomId, long sequence, String eventType, Object payload) {
        append(roomId, sequence, "PUBLIC", 0L, eventType, payload);
    }

    @Override
    public void appendPrivate(long roomId, long sequence, long ownerPlayerId, String eventType, Object payload) {
        if (ownerPlayerId <= 0L) throw new IllegalArgumentException("private event owner is required");
        append(roomId, sequence, "PLAYER_PRIVATE", ownerPlayerId, eventType, payload);
    }

    @Override
    public List<PerspectiveRoomEvent> after(long roomId, long authenticatedPlayerId,
            long sequenceExclusive, int requestedLimit) {
        if (roomId <= 0 || authenticatedPlayerId <= 0 || sequenceExclusive < 0)
            throw new IllegalArgumentException("invalid perspective event query");
        int limit = Math.max(1, Math.min(requestedLimit, MAX_PAGE_SIZE));
        String sql = "SELECT event_sequence,event_type,event_payload FROM aoo_room_event "
                + "WHERE room_id=? AND event_sequence>? AND ((visibility='PUBLIC' AND owner_player_id=0) "
                + "OR (visibility='PLAYER_PRIVATE' AND owner_player_id=?)) "
                + "ORDER BY event_sequence LIMIT ?";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId);
            statement.setLong(2, sequenceExclusive);
            statement.setLong(3, authenticatedPlayerId);
            statement.setInt(4, limit);
            List<PerspectiveRoomEvent> events = new ArrayList<>();
            try (var rows = statement.executeQuery()) {
                while (rows.next()) events.add(new PerspectiveRoomEvent(rows.getLong(1), rows.getString(2),
                        mapper.readValue(rows.getString(3), Object.class)));
            }
            return List.copyOf(events);
        } catch (Exception error) {
            throw new IllegalStateException("cannot load perspective room events", error);
        }
    }

    public List<PerspectiveRoomEvent> after(com.aoo.bcg.gamespi.AuthenticatedViewerScope viewer,
            long sequenceExclusive, int requestedLimit) {
        Objects.requireNonNull(viewer, "viewer");
        return after(viewer.roomId(), viewer.playerId(), sequenceExclusive, requestedLimit);
    }

    private void append(long roomId, long sequence, String visibility, long ownerPlayerId,
            String eventType, Object payload) {
        if (roomId <= 0 || sequence < 0 || eventType == null || eventType.isBlank())
            throw new IllegalArgumentException("invalid perspective event");
        String serialized;
        try { serialized = mapper.writeValueAsString(payload); }
        catch (Exception error) { throw new IllegalArgumentException("invalid perspective event payload", error); }
        String sql = "INSERT IGNORE INTO aoo_room_event(room_id,round_no,event_sequence,business_event_id,event_type,schema_version,event_payload,created_at,visibility,owner_player_id) "
                + "VALUES(?,0,?,?,?,1,?,CURRENT_TIMESTAMP(3),?,?)";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId); statement.setLong(2, sequence);
            statement.setString(3, "perspective:"+sequence+":"+visibility+":"+ownerPlayerId);
            statement.setString(4, eventType); statement.setString(5, serialized);
            statement.setString(6, visibility); statement.setLong(7, ownerPlayerId);
            if (statement.executeUpdate() == 1) return;
            try (var existing = connection.prepareStatement(
                    "SELECT event_type,event_payload FROM aoo_room_event WHERE room_id=? AND event_sequence=? AND visibility=? AND owner_player_id=?")) {
                existing.setLong(1, roomId); existing.setLong(2, sequence);
                existing.setString(3, visibility); existing.setLong(4, ownerPlayerId);
                try (var rows = existing.executeQuery()) {
                    if (!rows.next() || !eventType.equals(rows.getString(1))
                            || !mapper.readTree(serialized).equals(mapper.readTree(rows.getString(2))))
                        throw new IllegalStateException("event identity reused with different content");
                }
            }
        } catch (Exception error) {
            if (error instanceof IllegalStateException state) throw state;
            throw new IllegalStateException("cannot append perspective room event", error);
        }
    }
}
