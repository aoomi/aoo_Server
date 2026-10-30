package com.aoo.bcg.common.observability;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.junit.jupiter.api.Assertions.*;

class OperationLogContextTest {
    @Test void installsAndRestoresCompleteCorrelationScope() {
        try (var outer = OperationLogContext.open("outer-trace", "outer-request", 7, "outer-user", 0, "outer-conn");
             var ignored = OperationLogContext.open("trace-1", "request-1", 42, "user-7", 2, "conn-9")) {
            assertEquals("trace-1", OperationLogContext.current().get("traceId"));
            assertEquals("request-1", OperationLogContext.current().get("requestId"));
            assertEquals("42", OperationLogContext.current().get("roomId"));
            assertEquals("user-7", OperationLogContext.current().get("actorId"));
            assertEquals("2", OperationLogContext.current().get("seatId"));
            assertEquals("conn-9", OperationLogContext.current().get("connectionId"));
        }
        assertTrue(OperationLogContext.current().isEmpty());
    }

    @Test void rejectsPartialContext() {
        assertThrows(IllegalArgumentException.class,
                () -> OperationLogContext.open("", "request", 1, "user", 0, "connection"));
    }
}
