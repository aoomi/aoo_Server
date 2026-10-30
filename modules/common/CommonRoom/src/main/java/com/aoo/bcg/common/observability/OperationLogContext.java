package com.aoo.bcg.common.observability;

import org.slf4j.MDC;

import java.util.Map;

/** Scoped correlation context for one authoritative request. */
public final class OperationLogContext implements AutoCloseable {
    private static final ThreadLocal<Map<String, String>> CURRENT = new ThreadLocal<>();
    private final Map<String, String> previous;
    private final Map<String, String> previousCurrent;

    private OperationLogContext(Map<String, String> values) {
        previous = MDC.getCopyOfContextMap();
        previousCurrent = CURRENT.get();
        CURRENT.set(Map.copyOf(values));
        values.forEach(MDC::put);
    }

    public static Map<String, String> current() {
        Map<String, String> values = CURRENT.get();
        return values == null ? Map.of() : values;
    }

    public static OperationLogContext open(String traceId, String requestId, long roomId,
                                           String userId, int seatId, String connectionId) {
        if (traceId == null || traceId.isBlank() || requestId == null || requestId.isBlank()
                || roomId <= 0 || userId == null || userId.isBlank() || seatId < 0
                || connectionId == null || connectionId.isBlank()) {
            throw new IllegalArgumentException("complete operation log context is required");
        }
        return new OperationLogContext(Map.of(
                "traceId", traceId, "requestId", requestId, "roomId", Long.toString(roomId),
                "actorId", userId, "seatId", Integer.toString(seatId), "connectionId", connectionId));
    }

    @Override public void close() {
        MDC.clear();
        if (previous != null) MDC.setContextMap(previous);
        if (previousCurrent == null) CURRENT.remove(); else CURRENT.set(previousCurrent);
    }
}
