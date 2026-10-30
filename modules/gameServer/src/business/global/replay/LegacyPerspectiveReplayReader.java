package business.global.replay;

import core.db.DataBaseMgr;
import jsproto.c2s.cclass.playback.PerspectiveReplayPage;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class LegacyPerspectiveReplayReader {
    private static final LegacyPerspectiveReplayReader INSTANCE = new LegacyPerspectiveReplayReader();
    private LegacyPerspectiveReplayReader() {}
    public static LegacyPerspectiveReplayReader getInstance() { return INSTANCE; }

    public PerspectiveReplayPage read(long roomId, int setId, long authenticatedPlayerId,
            long afterSequence, int limit) {
        if (roomId <= 0 || setId < 0 || authenticatedPlayerId <= 0 || afterSequence < 0
                || limit < 1 || limit > 500) throw new IllegalArgumentException("invalid replay cursor");
        if (!LegacyReplayParticipantRegistry.getInstance().mayView(roomId, setId, authenticatedPlayerId)) {
            throw new SecurityException("replay access denied");
        }
        String sql = "SELECT event_sequence,message_id,payload FROM perspective_replay_event "
                + "WHERE room_id=? AND set_id=? AND event_sequence>? AND "
                + "((visibility='PUBLIC' AND owner_player_id=0) OR "
                + "(visibility='PLAYER_PRIVATE' AND owner_player_id=?)) "
                + "ORDER BY event_sequence LIMIT ?";
        try (var connection = DataBaseMgr.get("clark_game").getConnection();
             var statement = connection.prepareStatement(sql)) {
            statement.setLong(1, roomId); statement.setInt(2, setId);
            statement.setLong(3, afterSequence); statement.setLong(4, authenticatedPlayerId);
            statement.setInt(5, limit + 1);
            List<PerspectiveReplayPage.Event> items = new ArrayList<>();
            try (var rows = statement.executeQuery()) {
                while (rows.next()) items.add(new PerspectiveReplayPage.Event(rows.getLong(1),
                        rows.getString(2), new String(rows.getBytes(3), StandardCharsets.UTF_8)));
            }
            boolean hasMore = items.size() > limit;
            if (hasMore) items = new ArrayList<>(items.subList(0, limit));
            long next = items.isEmpty() ? afterSequence : items.get(items.size() - 1).getSequence();
            return new PerspectiveReplayPage(items, next, hasMore);
        } catch (Exception error) {
            throw new IllegalStateException("cannot read perspective replay", error);
        }
    }
}
