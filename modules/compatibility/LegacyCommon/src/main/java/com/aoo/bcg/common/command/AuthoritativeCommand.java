package com.aoo.bcg.common.command;

import java.time.Instant;
import java.util.Map;

public record AuthoritativeCommand(String requestId, long sequence, long playerId, long roomId, int roundNo,
                                   String playVersion, Instant occurredAt, String operation,
                                   Map<String, Object> intent) {
    public AuthoritativeCommand {
        if (requestId == null || requestId.isBlank() || playerId <= 0 || roomId <= 0 || sequence < 0 || roundNo < 0) throw new IllegalArgumentException("invalid command identity");
        if (playVersion == null || playVersion.isBlank() || operation == null || operation.isBlank() || occurredAt == null) throw new IllegalArgumentException("incomplete command");
        intent = Map.copyOf(intent == null ? Map.of() : intent);
    }
}
