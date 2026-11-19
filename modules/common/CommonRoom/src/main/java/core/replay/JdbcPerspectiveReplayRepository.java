package core.replay;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JdbcPerspectiveReplayRepository implements PerspectiveReplayRepository {
    private final DataSource dataSource;

    public JdbcPerspectiveReplayRepository(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public void append(PerspectiveReplayEvent event) {
        String sql = "INSERT INTO perspective_replay_event(room_id,set_id,event_sequence,visibility,owner_player_id,message_id,schema_version,play_version,payload) VALUES(?,?,?,?,?,?,?,?,?)";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, event.getRoomId());
            statement.setInt(2, event.getSetId());
            statement.setLong(3, event.getSequence());
            statement.setString(4, event.getVisibility().name());
            statement.setLong(5, event.getOwnerPlayerId());
            statement.setString(6, event.getMessageId());
            statement.setInt(7,event.getSchemaVersion());statement.setString(8,event.getPlayVersion());statement.setBytes(9, event.getPayload());
            statement.executeUpdate();
        } catch (java.sql.SQLException error) {
            if (error.getErrorCode() == 1062) return;
            throw new IllegalStateException("cannot append replay event", error);
        }
    }

    @Override
    public List<PerspectiveReplayEvent> findPublic(long roomId, int setId) {
        return find(roomId, setId, ReplayEventVisibility.PUBLIC, 0);
    }

    @Override
    public List<PerspectiveReplayEvent> findPrivate(long roomId, int setId, long playerId) {
        return find(roomId, setId, ReplayEventVisibility.PLAYER_PRIVATE, playerId);
    }

    private List<PerspectiveReplayEvent> find(long roomId, int setId,
            ReplayEventVisibility visibility, long ownerPlayerId) {
        String sql = "SELECT event_sequence,message_id,payload,schema_version,play_version FROM ("
                + "SELECT event_sequence,message_id,payload,schema_version,play_version,room_id,set_id,visibility,owner_player_id FROM perspective_replay_event UNION ALL "
                + "SELECT event_sequence,message_id,payload,schema_version,play_version,room_id,set_id,visibility,owner_player_id FROM perspective_replay_event_archive) replay "
                + "WHERE room_id=? AND set_id=? AND visibility=? AND owner_player_id=? ORDER BY event_sequence";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId); statement.setInt(2, setId);
            statement.setString(3, visibility.name()); statement.setLong(4, ownerPlayerId);
            List<PerspectiveReplayEvent> result = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) result.add(new PerspectiveReplayEvent(roomId, setId,
                        rows.getLong(1), visibility, ownerPlayerId, rows.getString(2), rows.getBytes(3),rows.getInt(4),rows.getString(5)));
            }
            return List.copyOf(result);
        } catch (java.sql.SQLException error) {
            throw new IllegalStateException("cannot load replay events", error);
        }
    }
}
