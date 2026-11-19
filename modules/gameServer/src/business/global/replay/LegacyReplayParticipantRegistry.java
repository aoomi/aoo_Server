package business.global.replay;

import core.db.DataBaseMgr;
import core.replay.ReplayParticipantRegistry;

public final class LegacyReplayParticipantRegistry implements ReplayParticipantRegistry {
    private static final LegacyReplayParticipantRegistry INSTANCE = new LegacyReplayParticipantRegistry();
    private LegacyReplayParticipantRegistry() {}
    public static LegacyReplayParticipantRegistry getInstance() { return INSTANCE; }

    @Override
    public void grant(long roomId, int setId, long playerId, int seatId) {
        if (roomId <= 0 || setId < 0 || playerId <= 0 || seatId < 0) return;
        String sql = "INSERT INTO replay_participant(room_id,set_id,player_id,seat_id,granted_at) "
                + "VALUES(?,?,?,?,CURRENT_TIMESTAMP(3)) ON DUPLICATE KEY UPDATE seat_id=VALUES(seat_id)";
        try (var connection = DataBaseMgr.get("clark_game").getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId); statement.setInt(2, setId);
            statement.setLong(3, playerId); statement.setInt(4, seatId); statement.executeUpdate();
        } catch (Exception error) {
            throw new IllegalStateException("cannot register replay participant", error);
        }
    }

    public void grantAll(long roomId, int setId, java.util.Map<Long, Integer> seats) {
        if (roomId <= 0 || setId < 0 || seats == null || seats.isEmpty()) return;
        String sql = "INSERT INTO replay_participant(room_id,set_id,player_id,seat_id,granted_at) "
                + "VALUES(?,?,?,?,CURRENT_TIMESTAMP(3)) ON DUPLICATE KEY UPDATE seat_id=VALUES(seat_id)";
        try (var connection = DataBaseMgr.get("clark_game").getConnection();
             var statement = connection.prepareStatement(sql)) {
            for (var seat : seats.entrySet()) {
                if (seat.getKey() <= 0 || seat.getValue() < 0) continue;
                statement.setLong(1, roomId); statement.setInt(2, setId);
                statement.setLong(3, seat.getKey()); statement.setInt(4, seat.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (Exception error) {
            throw new IllegalStateException("cannot register replay participants", error);
        }
    }

    @Override
    public boolean mayView(long roomId, int setId, long authenticatedPlayerId) {
        if (roomId <= 0 || setId < 0 || authenticatedPlayerId <= 0) return false;
        String sql = "SELECT 1 FROM replay_participant WHERE room_id=? AND set_id=? AND player_id=?";
        try (var connection = DataBaseMgr.get("clark_game").getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId); statement.setInt(2, setId); statement.setLong(3, authenticatedPlayerId);
            try (var row = statement.executeQuery()) { return row.next(); }
        } catch (Exception error) {
            throw new IllegalStateException("cannot authorize replay participant", error);
        }
    }

    @Override
    public boolean mayView(long roomId, int setId, long authenticatedPlayerId, int authenticatedSeatId) {
        if (roomId <= 0 || setId < 0 || authenticatedPlayerId <= 0 || authenticatedSeatId < 0) return false;
        String sql = "SELECT 1 FROM replay_participant WHERE room_id=? AND set_id=? AND player_id=? AND seat_id=?";
        try (var connection = DataBaseMgr.get("clark_game").getConnection(); var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId); statement.setInt(2, setId); statement.setLong(3, authenticatedPlayerId); statement.setInt(4, authenticatedSeatId);
            try (var row = statement.executeQuery()) { return row.next(); }
        } catch (Exception error) { throw new IllegalStateException("cannot authorize replay participant seat", error); }
    }
}
