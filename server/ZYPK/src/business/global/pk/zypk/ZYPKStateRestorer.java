package business.global.pk.zypk;

import com.aoo.bcg.common.recovery.RoomSnapshot;
import com.aoo.bcg.common.recovery.RoomStateRestorer;

import java.util.List;
import java.util.Map;

/** Rebuilds the ZYPK aggregate from a validated server-only snapshot. */
public final class ZYPKStateRestorer implements RoomStateRestorer<ZYPKTable, Map<String, Object>> {
    @Override
    public ZYPKTable fromSnapshot(RoomSnapshot snapshot) {
        if (snapshot.gameId() != ZYPKPersistenceService.GAME_ID)
            throw new IllegalArgumentException("snapshot gameId is not ZYPK");
        if (!ZYPKPersistenceService.PLAY_VERSION.equals(snapshot.playVersion()))
            throw new IllegalArgumentException("unsupported ZYPK play version");
        if (!ZYPKPersistenceService.COMPONENT_VERSION.equals(snapshot.componentVersion()))
            throw new IllegalArgumentException("unsupported ZYPK component version");
        return ZYPKTable.restore(snapshot.roomId(), snapshot.authoritativeState());
    }

    @Override
    public ZYPKTable replay(ZYPKTable state, List<Map<String, Object>> events) {
        ZYPKTable current = state;
        for (Map<String, Object> event : events) {
            Object eventType = event.get("eventType");
            Object stateAfter = event.get("stateAfter");
            if (!(eventType instanceof String type) || type.isBlank() || !(stateAfter instanceof Map<?, ?> rawState))
                throw new IllegalArgumentException("invalid ZYPK recovery event envelope");
            current = ZYPKTable.restore(current.roomId(), stringKeyMap(rawState));
        }
        return current;
    }

    @Override
    public String digest(ZYPKTable state) {
        return Integer.toUnsignedString(state.authoritativeState().hashCode(), 16);
    }

    private static Map<String, Object> stringKeyMap(Map<?, ?> source) {
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }
}
