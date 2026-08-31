package com.aoo.bcg.gateway;

import com.aoo.bcg.gamespi.time.AuthoritativeTimeSource;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class GameOperationTimelineTest {
    @Test void recordsBoundedReconstructableTimeline() {
        var timeline = new GameOperationTimeline(8);
        for (int i = 1; i <= 10; i++) {
            timeline.begin(42, i, "r-" + i, "play");
            timeline.complete(42, i, "r-" + i, "play",
                    OperationDeadline.open("play", 2, Duration.ofSeconds(10), AuthoritativeTimeSource.systemUtc()));
        }
        var entries = timeline.snapshot(42);
        assertEquals(8, entries.size());
        assertEquals(7, entries.getFirst().eventSeq());
        assertEquals(2, entries.getLast().opPos());
        assertTrue(entries.getLast().deadlineEpochMillis() > 0);
        assertNotNull(entries.getLast().threadState());
    }
}
