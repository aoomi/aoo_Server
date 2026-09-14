package com.aoo.bcg.gamespi.time;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Map;

/** Persistable authority for the currently open player-operation window. */
public record OperationDeadline(String operationId, int seatId, Instant deadline) {
    private static final Instant NONE = Instant.EPOCH;

    public OperationDeadline {
        operationId = Objects.requireNonNull(operationId, "operationId");
        deadline = Objects.requireNonNull(deadline, "deadline");
        if (operationId.isBlank() != deadline.equals(NONE) || (!operationId.isBlank() && seatId < 0))
            throw new IllegalArgumentException("invalid operation deadline");
    }

    public static OperationDeadline none() { return new OperationDeadline("", -1, NONE); }
    public static OperationDeadline open(String operationId, int seatId, Duration timeout, AuthoritativeTimeSource time) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isZero() || timeout.isNegative()) throw new IllegalArgumentException("timeout must be positive");
        return new OperationDeadline(operationId, seatId, time.deadlineAfter(timeout));
    }
    public boolean open() { return !operationId.isBlank(); }
    public boolean expired(AuthoritativeTimeSource time) { return open() && !time.now().isBefore(deadline); }
    public Map<String, Object> toMap() {
        return Map.of("operationId", operationId, "seatId", seatId, "deadlineEpochMillis", deadline.toEpochMilli());
    }
    public static OperationDeadline from(Object value) {
        if (!(value instanceof Map<?, ?> map)) return none();
        Object rawOperationId = map.get("operationId");
        String operationId = rawOperationId == null ? "" : String.valueOf(rawOperationId);
        Object rawSeat = map.get("seatId");
        Object rawDeadline = map.get("deadlineEpochMillis");
        int seatId = rawSeat instanceof Number number ? number.intValue() : -1;
        long epochMillis = rawDeadline instanceof Number number ? number.longValue() : 0L;
        return operationId.isBlank() ? none() : new OperationDeadline(operationId, seatId, Instant.ofEpochMilli(epochMillis));
    }
}
