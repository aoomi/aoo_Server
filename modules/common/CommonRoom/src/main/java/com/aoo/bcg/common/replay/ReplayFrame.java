package com.aoo.bcg.common.replay;
import com.aoo.bcg.gamespi.ImmutableValue;
import java.time.Instant;
public record ReplayFrame(long sequence, Instant occurredAt, String operation, Object publicPayload) {
    public ReplayFrame {
        if (sequence < 0 || occurredAt == null || operation == null || operation.isBlank())
            throw new IllegalArgumentException("invalid replay frame");
        publicPayload = ImmutableValue.freeze(publicPayload);
    }
}
