package com.aoo.bcg.gamespi;

/** Trusted Hall-to-game boundary for room-specific IP and location admission rules. */
public interface RoomAdmissionAuthority {
    record Admission(String ipAddress, Double latitude, Double longitude) { }

    void admitParticipant(long playerId, int seatId, Admission admission);
}
