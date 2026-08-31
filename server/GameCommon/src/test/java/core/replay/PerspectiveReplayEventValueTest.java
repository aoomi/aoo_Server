package core.replay;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PerspectiveReplayEventValueTest {
    @Test void equalEventsHaveEqualHashesAndDetachedPayloads() {
        byte[] payload = {1, 2, 3};
        PerspectiveReplayEvent left = new PerspectiveReplayEvent(1, 2, 3,
                ReplayEventVisibility.PUBLIC, 0, "played", payload);
        PerspectiveReplayEvent right = new PerspectiveReplayEvent(1, 2, 3,
                ReplayEventVisibility.PUBLIC, 0, "played", new byte[]{1, 2, 3});
        payload[0] = 9;
        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
        assertArrayEquals(new byte[]{1, 2, 3}, left.getPayload());
    }
}
