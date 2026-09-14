package com.aoo.bcg.gamespi.time;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Linearization point shared by a player's last-moment command and its timeout task. */
public final class OperationDeadlineArbiter {
    public enum Outcome { PLAYER, TIMEOUT }
    public record Resolution(String operationId, Outcome outcome, String requestId, Instant resolvedAt) { }
    public record PlayerResult<T>(Resolution resolution, T value) { }
    private final ConcurrentHashMap<String, Resolution> resolutions = new ConcurrentHashMap<>();

    public synchronized <T> PlayerResult<T> resolvePlayer(OperationDeadline window, String requestId,
            AuthoritativeTimeSource time, Supplier<T> acceptedMutation) {
        require(window, time); Objects.requireNonNull(requestId, "requestId"); Objects.requireNonNull(acceptedMutation, "acceptedMutation");
        Resolution previous = resolutions.get(window.operationId());
        if (previous != null) throw new IllegalStateException("operation already resolved as " + previous.outcome());
        Instant now = time.now();
        if (!now.isBefore(window.deadline())) {
            resolutions.put(window.operationId(), new Resolution(window.operationId(), Outcome.TIMEOUT, "", now));
            throw new IllegalStateException("operation deadline expired");
        }
        T value = acceptedMutation.get();
        Resolution resolution = new Resolution(window.operationId(), Outcome.PLAYER, requestId, now);
        resolutions.put(window.operationId(), resolution);
        return new PlayerResult<>(resolution, value);
    }

    public synchronized Resolution resolveTimeout(OperationDeadline window, AuthoritativeTimeSource time, Runnable timeoutMutation) {
        require(window, time); Objects.requireNonNull(timeoutMutation, "timeoutMutation");
        Resolution previous = resolutions.get(window.operationId());
        if (previous != null) return previous;
        Instant now = time.now();
        if (now.isBefore(window.deadline())) throw new IllegalStateException("deadline not reached");
        timeoutMutation.run();
        Resolution resolution = new Resolution(window.operationId(), Outcome.TIMEOUT, "", now);
        resolutions.put(window.operationId(), resolution);
        return resolution;
    }

    public void forget(String operationId) { resolutions.remove(operationId); }
    private static void require(OperationDeadline window, AuthoritativeTimeSource time) {
        Objects.requireNonNull(window, "window"); Objects.requireNonNull(time, "time");
        if (!window.open()) throw new IllegalArgumentException("operation window is closed");
    }
}
