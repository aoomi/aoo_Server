package com.aoo.bcg.lobby;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LobbyHttpRoomAuthorityPortErrorContractTest {
    @Test void retriesFiveHundredButPreservesUpstreamWireRootAndCanonicalCategory() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 4);
        server.createContext("/internal/v1/room-authority/leave", exchange -> {
            calls.incrementAndGet();
            byte[] body = "{\"code\":\"HALL_COST_POLICY_MISSING\",\"message\":\"active room cost policy is missing\"}"
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        try {
            LobbyHttpRoomAuthorityPort port = new LobbyHttpRoomAuthorityPort(
                    URI.create("http://127.0.0.1:" + server.getAddress().getPort()),
                    "authority-token", new ObjectMapper());
            LobbyRoomAuthorityRejected failure = org.junit.jupiter.api.Assertions.assertThrows(
                    LobbyRoomAuthorityRejected.class,
                    () -> port.leave(23, 7, "chengdu1001-v1", "leave-error", "trace-error",
                            Map.of("roomInstanceId", "room-instance")));
            assertEquals(503, failure.status());
            assertEquals("HALL_COST_POLICY_MISSING", failure.code());
            assertEquals("active room cost policy is missing", failure.getMessage());
            assertEquals("PRICING_POLICY_MISSING",
                    LobbyHttpRoomAuthorityPort.canonicalCode(failure.code(), failure.getMessage(), failure.status()));
            assertEquals(3, calls.get());
        } finally {
            server.stop(0);
        }
    }

    @Test void transportFailureRetainsCauseAndFailsClosedAsUpstream() {
        LobbyRoomAuthorityRejected failure = new LobbyRoomAuthorityRejected(
                503, "ROOM_AUTHORITY_UNAVAILABLE", "room authority unavailable: IOException: connection refused",
                new java.io.IOException("connection refused"));
        assertEquals("UPSTREAM_UNAVAILABLE",
                LobbyHttpRoomAuthorityPort.canonicalCode(failure.code(), failure.getMessage(), failure.status()));
        assertTrue(failure.getCause() instanceof java.io.IOException);
    }

    @Test void lobbyRouteCanonicalMappingKeepsHallCostAndIdentityRootsVisible() {
        assertEquals("PRICING_POLICY_MISSING",
                LobbyHttpRoutes.canonicalFailureCode(new IllegalStateException("HALL_COST_POLICY_MISSING")));
        assertEquals("IDENTITY_MISMATCH",
                LobbyHttpRoutes.canonicalFailureCode(new IllegalStateException("ROOM_ORCHESTRATOR_LEAVE_IDENTITY_REQUIRED")));
    }
}
