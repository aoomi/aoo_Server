package com.aoo.bcg.common.recovery;

import com.aoo.bcg.common.replay.ReplayFrame;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeepSnapshotIsolationTest {
    @Test void recoveryAndReplayDetachNestedCollections() {
        List<Integer> cards = new ArrayList<>(List.of(7, 8));
        Map<String, Object> nested = new LinkedHashMap<>(); nested.put("cards", cards);
        RoomSnapshot snapshot = new RoomSnapshot(1, 2, "v1", "c1", 1, 1, Instant.now(),
                Map.of("turn", nested));
        ReplayFrame replay = new ReplayFrame(1, Instant.now(), "play", nested);
        cards.add(9);
        assertEquals(List.of(7, 8), ((Map<?, ?>) snapshot.authoritativeState().get("turn")).get("cards"));
        assertEquals(List.of(7, 8), ((Map<?, ?>) replay.publicPayload()).get("cards"));
    }
}
