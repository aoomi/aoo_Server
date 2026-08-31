package business.global.pk.zjh;

import com.aoo.bcg.common.recovery.RoomSnapshot;
import com.aoo.bcg.common.recovery.RoomStateRestorer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Validates and rebuilds the authoritative ZJH aggregate from snapshots and journal envelopes. */
public final class ZJHStateRestorer implements RoomStateRestorer<ZJHTable, Map<String, Object>> {
    @Override public ZJHTable fromSnapshot(RoomSnapshot snapshot) {
        if (snapshot.gameId() != ZJHPersistenceService.GAME_ID) throw new IllegalArgumentException("snapshot gameId is not ZJH");
        if (!ZJHPersistenceService.PLAY_VERSION.equals(snapshot.playVersion())) throw new IllegalArgumentException("unsupported ZJH play version");
        if (!ZJHPersistenceService.COMPONENT_VERSION.equals(snapshot.componentVersion())) throw new IllegalArgumentException("unsupported ZJH component version");
        return ZJHTable.restore(snapshot.roomId(), snapshot.authoritativeState());
    }
    @Override public ZJHTable replay(ZJHTable state, List<Map<String, Object>> events) {
        ZJHTable current = state;
        for (Map<String, Object> event : events) {
            if (!(event.get("eventType") instanceof String type) || type.isBlank()
                    || !(event.get("stateAfter") instanceof Map<?, ?> raw))
                throw new IllegalArgumentException("invalid ZJH recovery event envelope");
            LinkedHashMap<String, Object> snapshot = new LinkedHashMap<>();
            raw.forEach((key, value) -> snapshot.put(String.valueOf(key), value));
            current = ZJHTable.restore(current.roomId(), snapshot);
        }
        return current;
    }
    @Override public String digest(ZJHTable state) { return Integer.toUnsignedString(state.authoritativeState().hashCode(), 16); }
}
