package business.global.pk.zjh;

import com.aoo.bcg.common.event.RoomEventJournal;
import com.aoo.bcg.common.event.RoomEventIdentity;
import com.aoo.bcg.common.event.JdbcRoomEventJournal;
import com.aoo.bcg.common.recovery.RoomSnapshot;
import com.aoo.bcg.common.recovery.RoomSnapshotStore;

import java.time.Clock;
import java.util.Map;

/** Persists authoritative ZJH events and recovery snapshots as one ordered boundary. */
public final class ZJHPersistenceService {
    public static final int GAME_ID = 9;
    public static final String PLAY_VERSION = "zjh-v1.0.0";
    public static final String COMPONENT_VERSION = "zjh-table-v1";

    private final RoomEventJournal events;
    private final RoomSnapshotStore snapshots;
    private final Clock clock;

    public ZJHPersistenceService(RoomEventJournal events, RoomSnapshotStore snapshots, Clock clock) {
        this.events = java.util.Objects.requireNonNull(events);
        this.snapshots = java.util.Objects.requireNonNull(snapshots);
        this.clock = java.util.Objects.requireNonNull(clock);
    }

    public void persist(ZJHTable table, int roundNo, long sequence, long fencingToken,
                        String businessEventId, String eventType, Map<String, Object> eventPayload) {
        if (sequence <= 0 || fencingToken <= 0) throw new IllegalArgumentException("positive sequence and fencing token required");
        if (eventType == null || eventType.isBlank()) throw new IllegalArgumentException("event type required");
        Map<String, Object> payload = Map.copyOf(eventPayload == null ? Map.of() : eventPayload);
        Map<String, Object> state = table.authoritativeState();
        Map<String, Object> envelope = Map.of(
                "eventType", eventType,
                "payload", payload,
                "stateAfter", state);
        RoomEventIdentity identity=RoomEventIdentity.v1(table.roomId(),roundNo,sequence,businessEventId,eventType);
        RoomSnapshot snapshot=new RoomSnapshot(table.roomId(), GAME_ID, PLAY_VERSION, COMPONENT_VERSION,
                fencingToken, sequence, clock.instant(), state);
        if(events instanceof JdbcRoomEventJournal jdbc) jdbc.appendAndSnapshot(snapshot,identity,envelope);
        else { events.append(identity,envelope); snapshots.save(snapshot); }
    }
}
