package com.aoo.bcg.hall;

import com.aoo.bcg.lobby.LobbyApplication;
import com.aoo.bcg.lobby.LobbyCapabilityCatalog;

/** Compatibility entry retained for callers that still use the Hall name. */
public final class HallApplication {
    private HallApplication() {}

    public static void start(String[] args) {
        LobbyApplication.start(args);
    }

    public static State state() {
        return LobbyApplication.State.STARTED == LobbyApplication.state() ? State.STARTED : State.STOPPED;
    }

    public enum State { STOPPED, STARTED }

    /** Compatibility alias for the canonical Lobby capability catalog. */
    public static final class HallCapabilityCatalog {
        private HallCapabilityCatalog() {}
        public static java.util.List<String> capabilities() {
            return LobbyCapabilityCatalog.capabilities();
        }

        static void requireComplete() {
            LobbyCapabilityCatalog.requireComplete();
        }
    }
}
