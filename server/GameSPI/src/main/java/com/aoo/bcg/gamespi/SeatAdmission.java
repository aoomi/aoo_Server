package com.aoo.bcg.gamespi;

import java.time.Instant;

/** Observable pre-seat lifecycle. ADMITTED is reached only after authority accepted join. */
public record SeatAdmission(long roomId, PlayerSeatIdentity identity, SeatAdmissionPhase phase,
                            Instant updatedAt, String failureCode) {
    public SeatAdmission {
        if (roomId <= 0 || identity == null || phase == null || updatedAt == null) {
            throw new IllegalArgumentException("invalid seat admission");
        }
        failureCode = failureCode == null ? "" : failureCode;
    }
}
