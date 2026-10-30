package com.aoo.bcg.lobby;

import java.util.concurrent.atomic.AtomicReference;

/** Canonical process-facing entry for the modern lobby application boundary. */
public final class LobbyApplication {
    private static final AtomicReference<State> STATE = new AtomicReference<>(State.STOPPED);

    private LobbyApplication() {}

    public static void start(String[] args) {
        if (!STATE.compareAndSet(State.STOPPED, State.STARTED)) {
            throw new IllegalStateException("lobby is already started");
        }
        LobbyCapabilityCatalog.requireComplete();
        Runtime.getRuntime().addShutdownHook(new Thread(
                () -> STATE.set(State.STOPPED), "aoo-lobby-shutdown"));
    }

    public static State state() {
        return STATE.get();
    }

    public enum State { STOPPED, STARTED }
}
