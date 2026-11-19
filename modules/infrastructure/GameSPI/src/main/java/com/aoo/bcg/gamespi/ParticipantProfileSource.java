package com.aoo.bcg.gamespi;

/** Server-owned participant profile lookup keyed by authenticated account identity. */
public interface ParticipantProfileSource {
    String nickname(long accountId);
}
