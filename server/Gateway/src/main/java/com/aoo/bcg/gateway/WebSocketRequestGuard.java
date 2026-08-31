package com.aoo.bcg.gateway;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

public final class WebSocketRequestGuard {
    private final Clock clock;
    private final long allowedSkewMillis;

    public WebSocketRequestGuard(Clock clock, Duration allowedClockSkew) {
        this.clock = Objects.requireNonNull(clock);
        if (allowedClockSkew == null || allowedClockSkew.isNegative() || allowedClockSkew.isZero()) {
            throw new IllegalArgumentException("allowed clock skew must be positive");
        }
        this.allowedSkewMillis = allowedClockSkew.toMillis();
    }

    public ConnectionSession validate(ConnectionSession session, WebSocketFrame frame) {
        Objects.requireNonNull(session);
        Objects.requireNonNull(frame);
        if (!session.roomId().equals(frame.roomId())) throw new SecurityException("room mismatch");
        if (!session.playVersion().equals(frame.playVersion())) throw new SecurityException("play version mismatch");
        long age = Math.abs(Math.subtractExact(clock.millis(), frame.timestamp()));
        if (age > allowedSkewMillis) throw new SecurityException("request timestamp outside accepted window");
        return session.accept(frame.seq());
    }
}
