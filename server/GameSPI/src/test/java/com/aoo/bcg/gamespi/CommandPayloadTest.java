package com.aoo.bcg.gamespi;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CommandPayloadTest {
    @Test void providesStrictTypedBoundaryAccess() {
        CommandPayload payload = CommandPayload.copyOf(Map.of(
                "seatId", 2L, "token", "abc", "cards", List.of(3L, 4)));
        assertEquals(2, payload.requireInt("seatId"));
        assertEquals("abc", payload.requireString("token"));
        assertEquals(List.of(3, 4), payload.requireIntList("cards"));
        assertThrows(IllegalArgumentException.class, () -> payload.requireInt("token"));
        assertThrows(IllegalArgumentException.class, () -> payload.requireLong("missing"));
        assertThrows(UnsupportedOperationException.class, () -> payload.put("x", 1));
        FieldKey<String> token = new FieldKey<>("token", String.class);
        assertEquals("abc", payload.require(token));
        assertTrue(payload.optional(new FieldKey<>("missing", String.class)).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> payload.require(new FieldKey<>("seatId", String.class)));
    }
}
