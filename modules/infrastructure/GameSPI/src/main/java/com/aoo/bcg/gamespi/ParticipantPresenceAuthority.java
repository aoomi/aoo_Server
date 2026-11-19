package com.aoo.bcg.gamespi;

import java.time.Instant;

/** Trusted transport-presence boundary for room rules that depend on offline duration. */
public interface ParticipantPresenceAuthority {
    void participantPresence(long playerId, boolean online, Instant observedAt);
}
