package com.aoo.bcg.common.perspective;

import com.aoo.bcg.common.event.OutboxBacklog;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import static org.junit.jupiter.api.Assertions.*;

class OptionalStateSemanticsTest {
    @Test void seatAbsenceIsExplicitAndConstrainedByAdmissionState() {
        assertDoesNotThrow(() -> new SpectatorAdmission(1, 2, SpectatorAdmission.State.SPECTATING,
                OptionalInt.empty(), 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new SpectatorAdmission(1, 2,
                SpectatorAdmission.State.RESERVED_FOR_NEXT_ROUND, OptionalInt.empty(), 1, 1));
    }

    @Test void emptyBacklogHasExplicitAbsentOldestTimestamp() {
        OutboxBacklog empty = new OutboxBacklog(0, 0, Optional.empty());
        assertTrue(empty.oldestCreatedAt().isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> new OutboxBacklog(1, 0, Optional.empty()));
        assertDoesNotThrow(() -> new OutboxBacklog(1, 0, Optional.of(Instant.now())));
    }
}
