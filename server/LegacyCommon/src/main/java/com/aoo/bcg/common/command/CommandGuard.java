package com.aoo.bcg.common.command;

import java.time.Clock;
import java.time.Duration;

public final class CommandGuard {
    private final Clock clock;
    private final Duration allowedSkew;
    public CommandGuard(Clock clock, Duration allowedSkew) { this.clock = clock; this.allowedSkew = allowedSkew; }
    public String rejectReason(AuthoritativeCommand command, long sessionPlayerId, long sessionRoomId,
                               String roomVersion, long lastSequence) {
        if (command.playerId() != sessionPlayerId) return "PLAYER_MISMATCH";
        if (command.roomId() != sessionRoomId) return "ROOM_MISMATCH";
        if (!command.playVersion().equals(roomVersion)) return "VERSION_MISMATCH";
        if (command.sequence() <= lastSequence) return "REPLAYED_SEQUENCE";
        if (Duration.between(command.occurredAt(), clock.instant()).abs().compareTo(allowedSkew) > 0) return "TIMESTAMP_OUT_OF_WINDOW";
        return "";
    }
}
