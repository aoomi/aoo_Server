package business.global.replay;

import com.ddm.server.common.redis.RedisUtil;
import com.ddm.server.common.CommLogD;
import com.google.gson.Gson;
import core.db.DataBaseMgr;
import core.replay.ReplayEventVisibility;
import jsproto.c2s.cclass.BaseSendMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/** Bounded asynchronous bridge from the legacy game loop to perspective replay storage. */
public final class LegacyPerspectiveReplayRecorder {
    private static final LegacyPerspectiveReplayRecorder INSTANCE = new LegacyPerspectiveReplayRecorder();
    private static final int CAPACITY = 20_000;
    private final ArrayBlockingQueue<Event> queue = new ArrayBlockingQueue<>(CAPACITY);
    private final Gson gson = new Gson();
    private final AtomicLong droppedEvents = new AtomicLong();

    private LegacyPerspectiveReplayRecorder() {
        Thread worker = new Thread(this::runWriter, "perspective-replay-writer");
        worker.setDaemon(true);
        worker.start();
    }

    public static LegacyPerspectiveReplayRecorder getInstance() { return INSTANCE; }

    public void recordPublic(long roomId, int setId, BaseSendMsg message) {
        enqueue(roomId, setId, ReplayEventVisibility.PUBLIC, 0, message);
    }

    public void recordPrivate(long roomId, int setId, long playerId, BaseSendMsg message) {
        if (playerId <= 0) return;
        enqueue(roomId, setId, ReplayEventVisibility.PLAYER_PRIVATE, playerId, message);
    }

    private void enqueue(long roomId, int setId, ReplayEventVisibility visibility,
            long ownerPlayerId, BaseSendMsg message) {
        if (roomId <= 0 || setId < 0 || message == null) return;
        long sequence = RedisUtil.incrLong("aoo:replay:sequence:" + roomId + ":" + setId);
        long roomSequence = RedisUtil.incrLong("aoo:room:event-sequence:" + roomId);
        if (sequence <= 0 || roomSequence <= 0) {
            droppedEvents.incrementAndGet();
            CommLogD.error("cannot allocate perspective replay sequence roomId:{},setId:{}", roomId, setId);
            return;
        }
        Event event = new Event(roomId, setId, sequence, roomSequence, visibility.name(), ownerPlayerId,
                message.getClass().getName(), gson.toJson(message).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        try {
            queue.put(event);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            droppedEvents.incrementAndGet();
            CommLogD.error("perspective replay enqueue interrupted roomId:{},setId:{}", roomId, setId);
        }
    }

    public int queueSize() { return queue.size(); }
    public long droppedEventCount() { return droppedEvents.get(); }

    private void runWriter() {
        List<Event> batch = new ArrayList<>(200);
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (batch.isEmpty()) {
                    batch.add(queue.take());
                    queue.drainTo(batch, 199);
                }
                writeBatch(batch);
                batch.clear();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            } catch (Exception error) {
                try { Thread.sleep(1_000L); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }
        }
    }

    private void writeBatch(List<Event> batch) throws Exception {
        String replaySql = "INSERT INTO perspective_replay_event(room_id,set_id,event_sequence,visibility,owner_player_id,message_id,schema_version,play_version,payload) "
                + "VALUES(?,?,?,?,?,?,1,?,?) ON DUPLICATE KEY UPDATE message_id=message_id";
        String eventSql = "INSERT INTO aoo_room_event(room_id,round_no,event_sequence,business_event_id,event_type,schema_version,event_payload,created_at,visibility,owner_player_id) "
                + "VALUES(?,?,?,?,?,1,?,CURRENT_TIMESTAMP(3),?,?) ON DUPLICATE KEY UPDATE event_sequence=event_sequence";
        try (var connection = DataBaseMgr.get("clark_game").getConnection();
             var replayStatement = connection.prepareStatement(replaySql);
             var eventStatement = connection.prepareStatement(eventSql)) {
            connection.setAutoCommit(false);
            try {
            for (Event event : batch) {
                replayStatement.setLong(1, event.roomId); replayStatement.setInt(2, event.setId);
                replayStatement.setLong(3, event.sequence); replayStatement.setString(4, event.visibility);
                replayStatement.setLong(5, event.ownerPlayerId); replayStatement.setString(6, event.messageId);
                replayStatement.setString(7,"legacy-"+event.messageId);replayStatement.setBytes(8, event.payload); replayStatement.addBatch();

                eventStatement.setLong(1, event.roomId);eventStatement.setInt(2,event.setId);eventStatement.setLong(3, event.roomSequence);
                eventStatement.setString(4,"legacy:"+event.setId+":"+event.roomSequence+":"+event.visibility+":"+event.ownerPlayerId);
                eventStatement.setString(5, event.messageId);
                eventStatement.setString(6, new String(event.payload, java.nio.charset.StandardCharsets.UTF_8));
                eventStatement.setString(7, event.visibility);
                eventStatement.setLong(8, event.ownerPlayerId); eventStatement.addBatch();
            }
            replayStatement.executeBatch();
            eventStatement.executeBatch();
            connection.commit();
            } catch(Exception error) { connection.rollback(); throw error; }
            finally { connection.setAutoCommit(true); }
        }
    }

    private record Event(long roomId, int setId, long sequence, long roomSequence, String visibility,
                         long ownerPlayerId, String messageId, byte[] payload) {}
}
