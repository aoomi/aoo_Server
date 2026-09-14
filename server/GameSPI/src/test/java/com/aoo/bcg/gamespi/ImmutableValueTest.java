package com.aoo.bcg.gamespi;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class ImmutableValueTest {
    @Test void recursivelyDetachesAndFreezesNestedValues() {
        List<Integer> cards = new ArrayList<>(List.of(1, 2));
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("state", Map.of("cards", cards));
        Map<String, Object> frozen = ImmutableValue.freezeStringMap(source);
        cards.add(3);
        assertEquals(List.of(1, 2), ((Map<?, ?>) frozen.get("state")).get("cards"));
        assertThrows(UnsupportedOperationException.class,
                () -> ((List<Object>) ((Map<?, ?>) frozen.get("state")).get("cards")).add(4));
    }

    @Test void rejectsCyclesAndUnknownMutableObjects() {
        List<Object> cycle = new ArrayList<>(); cycle.add(cycle);
        assertThrows(IllegalArgumentException.class, () -> ImmutableValue.freeze(cycle));
        assertThrows(IllegalArgumentException.class, () -> ImmutableValue.freeze(new StringBuilder("mutable")));
    }
}
