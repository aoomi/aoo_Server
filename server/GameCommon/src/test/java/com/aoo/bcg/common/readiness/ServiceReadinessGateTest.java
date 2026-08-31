package com.aoo.bcg.common.readiness;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ServiceReadinessGateTest {
    @Test void rejectsTrafficUntilAllDependenciesPass() {
        ServiceReadinessGate gate = new ServiceReadinessGate(List.of(
                ServiceReadinessGate.check("database", () -> {}),
                ServiceReadinessGate.check("gameCatalog", () -> { throw new IllegalStateException("empty"); })));

        assertThrows(ServiceUnavailableException.class, gate::requireAcceptingTraffic);
        IllegalStateException error = assertThrows(IllegalStateException.class, gate::verifyAndOpen);
        assertTrue(error.getMessage().contains("gameCatalog: empty"));
        assertEquals(ServiceReadinessGate.State.FAILED, gate.state());
        assertThrows(ServiceUnavailableException.class, gate::requireAcceptingTraffic);
    }

    @Test void opensOnlyAfterEveryCheckPasses() {
        ServiceReadinessGate gate = new ServiceReadinessGate(List.of(
                ServiceReadinessGate.check("database", () -> {}),
                ServiceReadinessGate.check("criticalDependency", () -> {})));
        gate.verifyAndOpen();
        assertDoesNotThrow(gate::requireAcceptingTraffic);
        assertEquals(ServiceReadinessGate.State.READY, gate.state());
        assertTrue(gate.failures().isEmpty());
    }
}
