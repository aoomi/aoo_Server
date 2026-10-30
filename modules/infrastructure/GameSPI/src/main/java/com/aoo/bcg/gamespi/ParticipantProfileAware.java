package com.aoo.bcg.gamespi;

/** Runtime injection point used after both new-room creation and durable restore. */
public interface ParticipantProfileAware {
    void attachParticipantProfileSource(ParticipantProfileSource source);
}
