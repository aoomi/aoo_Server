package com.aoo.bcg.common.random;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SeededGameRandomSourceTest {
    @Test void sameSeedReplaysDecisionsAndShuffle() {
        GameRandomSource first = new SeededGameRandomSource(20260822L);
        GameRandomSource replay = new SeededGameRandomSource(20260822L);
        List<Integer> firstCards = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
        List<Integer> replayCards = new ArrayList<>(firstCards);
        assertEquals(first.nextInt(100), replay.nextInt(100));
        assertEquals(first.nextBoolean(), replay.nextBoolean());
        first.shuffle(firstCards);
        replay.shuffle(replayCards);
        assertEquals(firstCards, replayCards);
        assertEquals(first.seed(), replay.seed());
    }

    @Test void generatedStreamsExposeDifferentAuditSeeds() {
        assertNotEquals(SeededGameRandomSource.create().seed(), SeededGameRandomSource.create().seed());
    }
}
