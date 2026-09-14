package business.global.replay;

import core.db.DataBaseMgr;
import jsproto.c2s.cclass.room.SRoom_ReconnectV2;

import java.util.ArrayList;
import java.util.List;

public final class LegacyRoomReconnectReader {
    private static final LegacyRoomReconnectReader INSTANCE = new LegacyRoomReconnectReader();
    private LegacyRoomReconnectReader() {}
    public static LegacyRoomReconnectReader getInstance() { return INSTANCE; }

    public List<SRoom_ReconnectV2.Event> read(long roomId, long authenticatedPlayerId,
            long sequenceExclusive, int limit) {
        return read(roomId,authenticatedPlayerId,sequenceExclusive,limit,true);
    }

    public List<SRoom_ReconnectV2.Event> read(long roomId, long authenticatedPlayerId,
            long sequenceExclusive, int limit, boolean includePlayerPrivate) {
        if (roomId <= 0 || authenticatedPlayerId <= 0 || sequenceExclusive < 0 || limit < 1 || limit > 501)
            throw new IllegalArgumentException("invalid reconnect event cursor");
        String visibility = includePlayerPrivate
                ? "((visibility='PUBLIC' AND owner_player_id=0) OR (visibility='PLAYER_PRIVATE' AND owner_player_id=?))"
                : "(visibility='PUBLIC' AND owner_player_id=0)";
        String sql = "SELECT event_sequence,event_type,event_payload FROM aoo_room_event WHERE room_id=? AND event_sequence>? AND "
                + visibility + " ORDER BY event_sequence,visibility,owner_player_id LIMIT ?";
        try (var connection = DataBaseMgr.get("clark_game").getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId);
            statement.setLong(2, sequenceExclusive);
            int index=3;if(includePlayerPrivate)statement.setLong(index++,authenticatedPlayerId);statement.setInt(index, limit);
            List<SRoom_ReconnectV2.Event> result = new ArrayList<>();
            try (var rows = statement.executeQuery()) {
                while (rows.next()) result.add(new SRoom_ReconnectV2.Event(
                        rows.getLong(1), rows.getString(2), rows.getString(3)));
            }
            return List.copyOf(result);
        } catch (Exception error) {
            throw new IllegalStateException("cannot read reconnect events", error);
        }
    }
}
