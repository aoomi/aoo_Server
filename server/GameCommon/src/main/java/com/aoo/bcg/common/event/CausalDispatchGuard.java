package com.aoo.bcg.common.event;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/** Rejects synchronous event feedback and bounds callback fan-out depth per thread. */
public final class CausalDispatchGuard {
    private final int maxDepth;
    private final ThreadLocal<ArrayDeque<String>> stack = ThreadLocal.withInitial(ArrayDeque::new);
    private final ThreadLocal<Set<String>> active = ThreadLocal.withInitial(HashSet::new);

    public CausalDispatchGuard(int maxDepth) {
        if (maxDepth < 1 || maxDepth > 64) throw new IllegalArgumentException("event depth must be 1..64");
        this.maxDepth = maxDepth;
    }

    public <T> T dispatch(String channel, String causalId, Supplier<T> callback) {
        Objects.requireNonNull(callback, "callback");
        String key = require(channel, "channel") + ':' + require(causalId, "causalId");
        ArrayDeque<String> path = stack.get(); Set<String> keys = active.get();
        if (path.size() >= maxDepth) throw new IllegalStateException("event causal depth exceeded: " + path);
        if (!keys.add(key)) throw new IllegalStateException("event causal cycle detected: " + key);
        path.addLast(key);
        try { return callback.get(); }
        finally {
            path.removeLast(); keys.remove(key);
            if (path.isEmpty()) { stack.remove(); active.remove(); }
        }
    }

    public void dispatch(String channel, String causalId, Runnable callback) {
        dispatch(channel, causalId, () -> { callback.run(); return null; });
    }

    private static String require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}
