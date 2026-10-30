package com.aoo.bcg.external;

import java.time.Clock;
import java.time.Duration;

final class CircuitBreaker {
    private enum State { CLOSED, OPEN, HALF_OPEN }
    private final int threshold;
    private final Duration openDuration;
    private final Clock clock;
    private State state = State.CLOSED;
    private int failures;
    private long reopenAtMillis;

    CircuitBreaker(int threshold, Duration openDuration, Clock clock) {
        this.threshold = threshold;
        this.openDuration = openDuration;
        this.clock = clock;
    }

    synchronized void acquire() {
        if (state == State.OPEN && clock.millis() >= reopenAtMillis) state = State.HALF_OPEN;
        else if (state != State.CLOSED) throw new ExternalPlatformException.CircuitOpen();
    }

    synchronized void success() { state = State.CLOSED; failures = 0; }

    synchronized void failure() {
        if (state == State.HALF_OPEN || ++failures >= threshold) {
            state = State.OPEN;
            reopenAtMillis = clock.millis() + openDuration.toMillis();
        }
    }
}
