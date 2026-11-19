package com.aoo.bcg.gamespi;

import java.time.Instant;

/** Optional authority contract for room-level leave, vote and terminal lifecycle state. */
public interface RoomLifecycleAuthority {
    String TERMINAL_FIELD = "roomTerminal";
    String TERMINAL_REASON_FIELD = "roomTerminalReason";

    /** Applies server-time lifecycle transitions such as dissolve-vote timeout. */
    boolean tickLifecycle(Instant now);
    boolean isTerminal();
    String terminalReason();
}
