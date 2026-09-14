package com.aoo.bcg.common.persistence;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PersistenceReadiness {
    private static final Set<PersistenceCapability> REQUIRED =
            Set.copyOf(EnumSet.allOf(PersistenceCapability.class));

    private PersistenceReadiness() {
    }

    public static List<String> productionViolations(Collection<PersistenceBinding> bindings) {
        Map<PersistenceCapability, PersistenceBinding> byCapability = new EnumMap<>(PersistenceCapability.class);
        for (PersistenceBinding binding : bindings) {
            PersistenceBinding previous = byCapability.putIfAbsent(binding.capability(), binding);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate persistence capability: " + binding.capability());
            }
        }

        return REQUIRED.stream()
                .filter(capability -> {
                    PersistenceBinding binding = byCapability.get(capability);
                    return binding == null || !binding.durable() || !binding.distributed();
                })
                .map(capability -> violation(capability, byCapability.get(capability)))
                .toList();
    }

    public static void requireProductionReady(Collection<PersistenceBinding> bindings) {
        List<String> violations = productionViolations(bindings);
        if (!violations.isEmpty()) {
            throw new IllegalStateException("production persistence is not ready: " + String.join(", ", violations));
        }
    }

    private static String violation(PersistenceCapability capability, PersistenceBinding binding) {
        if (binding == null) {
            return capability + " is missing";
        }
        if (!binding.durable() && !binding.distributed()) {
            return capability + " is neither durable nor distributed (" + binding.implementation() + ")";
        }
        if (!binding.durable()) {
            return capability + " is not durable (" + binding.implementation() + ")";
        }
        return capability + " is not distributed (" + binding.implementation() + ")";
    }
}
