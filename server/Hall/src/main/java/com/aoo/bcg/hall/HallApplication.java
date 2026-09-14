package com.aoo.bcg.hall;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Sole process-facing entry for the modern hall application boundary. */
public final class HallApplication {
    private static final AtomicReference<State> STATE = new AtomicReference<>(State.STOPPED);

    private HallApplication() {}

    public static void start(String[] args) {
        if (!STATE.compareAndSet(State.STOPPED, State.STARTED)) {
            throw new IllegalStateException("hall is already started");
        }
        HallCapabilityCatalog.requireComplete();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> STATE.set(State.STOPPED), "aoo-hall-shutdown"));
    }

    public static State state() { return STATE.get(); }

    public enum State { STOPPED, STARTED }

    public static final class HallCapabilityCatalog {
        private static final List<String> CAPABILITIES = List.of(
                "account-session", "game-catalog", "room-management", "club-management");
        private HallCapabilityCatalog() {}
        public static List<String> capabilities() { return CAPABILITIES; }
        static void requireComplete() {
            if (CAPABILITIES.size() != 4) throw new IllegalStateException("hall capability ownership is incomplete");
        }
    }
}
