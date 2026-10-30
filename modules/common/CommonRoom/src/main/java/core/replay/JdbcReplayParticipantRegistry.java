package core.replay;

import javax.sql.DataSource;
import java.util.Objects;

public final class JdbcReplayParticipantRegistry implements ReplayParticipantRegistry {
    private final DataSource dataSource;

    public JdbcReplayParticipantRegistry(DataSource dataSource) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
    }

    @Override
    public void grant(long roomId, int setId, long playerId, int seatId) {
        if (roomId <= 0 || setId < 0 || playerId <= 0 || seatId < 0) {
            throw new IllegalArgumentException("invalid replay participant");
        }
        String sql = "INSERT INTO replay_participant(room_id,set_id,player_id,seat_id,granted_at) "
                + "VALUES(?,?,?,?,CURRENT_TIMESTAMP(3)) ON DUPLICATE KEY UPDATE seat_id=VALUES(seat_id)";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId); statement.setInt(2, setId);
            statement.setLong(3, playerId); statement.setInt(4, seatId);
            statement.executeUpdate();
        } catch (java.sql.SQLException error) {
            throw new IllegalStateException("cannot grant replay participant", error);
        }
    }

    @Override
    public boolean mayView(long roomId, int setId, long authenticatedPlayerId) {
        if (roomId <= 0 || setId < 0 || authenticatedPlayerId <= 0) return false;
        String sql = "SELECT 1 FROM (SELECT room_id,set_id,player_id FROM replay_participant UNION ALL SELECT room_id,set_id,player_id FROM replay_participant_archive) p WHERE room_id=? AND set_id=? AND player_id=?";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId); statement.setInt(2, setId); statement.setLong(3, authenticatedPlayerId);
            try (var row = statement.executeQuery()) { return row.next(); }
        } catch (java.sql.SQLException error) {
            throw new IllegalStateException("cannot authorize replay participant", error);
        }
    }

    @Override
    public boolean mayView(long roomId, int setId, long authenticatedPlayerId, int authenticatedSeatId) {
        if (roomId <= 0 || setId < 0 || authenticatedPlayerId <= 0 || authenticatedSeatId < 0) return false;
        String sql = "SELECT 1 FROM (SELECT room_id,set_id,player_id,seat_id FROM replay_participant UNION ALL SELECT room_id,set_id,player_id,seat_id FROM replay_participant_archive) p WHERE room_id=? AND set_id=? AND player_id=? AND seat_id=?";
        try (var connection = dataSource.getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId); statement.setInt(2, setId); statement.setLong(3, authenticatedPlayerId);
            statement.setInt(4, authenticatedSeatId);
            try (var row = statement.executeQuery()) { return row.next(); }
        } catch (java.sql.SQLException error) { throw new IllegalStateException("cannot authorize replay participant seat", error); }
    }
}
