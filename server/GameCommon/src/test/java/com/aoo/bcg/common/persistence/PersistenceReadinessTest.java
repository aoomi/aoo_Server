package com.aoo.bcg.common.persistence;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PersistenceReadinessTest {
    @Test
    void rejectsIncompleteAndInMemoryProductionBindings() {
        List<PersistenceBinding> bindings = List.of(
                PersistenceBinding.inMemory(PersistenceCapability.IDEMPOTENCY, Object.class));

        assertThrows(IllegalStateException.class,
                () -> PersistenceReadiness.requireProductionReady(bindings));
    }

    @Test
    void acceptsCompleteDurableDistributedBindings() {
        List<PersistenceBinding> bindings = Arrays.stream(PersistenceCapability.values())
                .map(capability -> new PersistenceBinding(capability, "production." + capability, true, true))
                .toList();

        assertDoesNotThrow(() -> PersistenceReadiness.requireProductionReady(bindings));
    }

    @Test
    void rejectsDuplicateCapabilityBindings() {
        PersistenceBinding binding = new PersistenceBinding(
                PersistenceCapability.OUTBOX, "production.Outbox", true, true);

        assertThrows(IllegalArgumentException.class,
                () -> PersistenceReadiness.productionViolations(List.of(binding, binding)));
    }
}
