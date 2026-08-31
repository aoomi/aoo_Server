package com.aoo.bcg.common.config;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Rejects partial mutation of runtime infrastructure; immutable play versions use their own publication flow. */
public final class ConfigReloadPolicy {
    private ConfigReloadPolicy() {}

    public static Set<RuntimeConfigKey> changed(Map<RuntimeConfigKey,String> active,
                                                Map<RuntimeConfigKey,String> candidate) {
        Set<RuntimeConfigKey> changed = new LinkedHashSet<>();
        for (RuntimeConfigKey key : RuntimeConfigKey.values())
            if (!Objects.equals(active.get(key), candidate.get(key))) changed.add(key);
        return Set.copyOf(changed);
    }

    public static void requireAtomicHotReload(Map<RuntimeConfigKey,String> active,
                                              Map<RuntimeConfigKey,String> candidate) {
        Set<RuntimeConfigKey> forbidden = new LinkedHashSet<>();
        for (RuntimeConfigKey key : changed(active, candidate))
            if (key.mutability() != RuntimeConfigKey.Mutability.ATOMIC_HOT_RELOAD) forbidden.add(key);
        if (!forbidden.isEmpty()) throw new IllegalStateException("configuration change requires restart: " + forbidden);
    }
}
