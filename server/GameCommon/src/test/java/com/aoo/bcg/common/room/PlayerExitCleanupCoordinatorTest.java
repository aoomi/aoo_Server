package com.aoo.bcg.common.room;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerExitCleanupCoordinatorTest {
    @Test void attemptsAllResourcesAndRetriesOnlyFailedCleanup() {
        PlayerExitCleanupCoordinator coordinator = new PlayerExitCleanupCoordinator();
        PlayerExitScope scope = new PlayerExitScope(1, 2, 0, 3); ArrayList<String> calls = new ArrayList<>();
        AtomicBoolean failTimer = new AtomicBoolean(true); Map<String, Runnable> steps = new LinkedHashMap<>();
        for (String name : java.util.List.of("connection", "seat", "candidates", "timers", "chatTargets", "clientMappingPush"))
            steps.put(name, () -> { calls.add(name); if (name.equals("timers") && failTimer.getAndSet(false)) throw new IllegalStateException("timer race"); });
        assertThrows(IllegalStateException.class, () -> coordinator.cleanup(scope, steps));
        assertEquals(java.util.Set.of("connection", "seat", "candidates", "chatTargets", "clientMappingPush"), coordinator.completedSteps(scope));
        coordinator.cleanup(scope, steps);
        assertEquals(7, calls.size()); assertTrue(coordinator.completedSteps(scope).isEmpty());
    }
}
