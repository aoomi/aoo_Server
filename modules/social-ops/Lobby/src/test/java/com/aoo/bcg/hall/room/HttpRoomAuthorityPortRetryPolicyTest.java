package com.aoo.bcg.hall.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class HttpRoomAuthorityPortRetryPolicyTest {
    @Test
    void deterministicNonRetryableAuthorityErrorIsSentOnce() throws Exception {
        try (Harness harness = new Harness(409, "ROOM_RUNTIME_IDENTITY_DESTROYED", 0)) {
            RoomAuthorityRejected failure = assertThrows(RoomAuthorityRejected.class,
                    () -> harness.port.leave(23, 7, "chengdu1001-v1", "leave-1", "trace-1",
                            Map.of("roomInstanceId", "room-instance")));
            assertEquals(409, failure.status());
            assertEquals("ROOM_RUNTIME_IDENTITY_DESTROYED", failure.code());
            assertEquals(1, harness.calls.get());
        }
    }

    @Test
    void temporaryFiveHundredRetriesAndCanRecover() throws Exception {
        try (Harness harness = new Harness(503, "UPSTREAM_UNAVAILABLE", 2)) {
            Map<String, Object> result = harness.port.leave(23, 7, "chengdu1001-v1", "leave-2", "trace-2",
                    Map.of("roomInstanceId", "room-instance"));
            assertEquals("LEFT", result.get("status"));
            assertEquals(2, harness.calls.get());
        }
    }

    @Test
    void unknownFiveHundredKeepsExistingThreeAttemptBound() throws Exception {
        try (Harness harness = new Harness(500, "INTERNAL_AUTHORITY_FAILED", 3)) {
            assertThrows(IllegalStateException.class,
                    () -> harness.port.leave(23, 7, "chengdu1001-v1", "leave-3", "trace-3",
                            Map.of("roomInstanceId", "room-instance")));
            assertEquals(3, harness.calls.get());
        }
    }

    private static final class Harness implements AutoCloseable {
        private final HttpServer server;
        private final AtomicInteger calls = new AtomicInteger();
        private final HttpRoomAuthorityPort port;
        private final int failureStatus;
        private final String failureCode;
        private final int successAt;

        private Harness(int failureStatus, String failureCode, int successAt) throws Exception {
            this.failureStatus = failureStatus;
            this.failureCode = failureCode;
            this.successAt = successAt;
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 4);
            server.createContext("/internal/v1/room-authority/leave", exchange -> {
                int attempt = calls.incrementAndGet();
                boolean success = successAt > 0 && attempt >= successAt && successAt < 3;
                int status = success ? 200 : failureStatus;
                String body = success ? "{\"code\":\"OK\",\"data\":{\"status\":\"LEFT\"}}"
                        : "{\"code\":\"" + failureCode + "\",\"message\":\"" + failureCode + "\"}";
                byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(status, bytes.length);
                try (var output = exchange.getResponseBody()) { output.write(bytes); }
            });
            server.start();
            port = new HttpRoomAuthorityPort(URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                    "authority-token", new ObjectMapper());
        }

        @Override public void close() { server.stop(0); }
    }
}
