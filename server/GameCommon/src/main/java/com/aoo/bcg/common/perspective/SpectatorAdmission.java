package com.aoo.bcg.common.perspective;

import java.util.OptionalInt;

public record SpectatorAdmission(long roomId, long playerId, State state, OptionalInt requestedSeatId,
                                 int effectiveRoundNo, long revision) {
    public enum State { SPECTATING, RESERVED_FOR_NEXT_ROUND, PLAYER }
    public SpectatorAdmission {
        if (roomId <= 0 || playerId <= 0 || state == null || requestedSeatId == null
                || effectiveRoundNo < 0 || revision < 0
                || requestedSeatId.isPresent() && requestedSeatId.getAsInt() < 0)
            throw new IllegalArgumentException("invalid spectator admission");
        if (state == State.SPECTATING && requestedSeatId.isPresent())
            throw new IllegalArgumentException("spectator cannot reserve a seat implicitly");
        if (state != State.SPECTATING && requestedSeatId.isEmpty())
            throw new IllegalArgumentException("reserved/player admission requires a seat");
    }
}
