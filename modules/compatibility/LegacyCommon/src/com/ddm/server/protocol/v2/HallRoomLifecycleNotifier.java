package com.ddm.server.protocol.v2;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Propagates an authoritative room removal to Hall without exposing a player endpoint. */
final class HallRoomLifecycleNotifier {
    private static final Logger LOG = Logger.getLogger(HallRoomLifecycleNotifier.class.getName());
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
    private static final int MAX_ATTEMPTS = 3;

    private HallRoomLifecycleNotifier() {}

    static void close(long roomId) {
        String base = first("hall.internal.url", "HALL_INTERNAL_URL", "GATEWAY_HALL_URL");
        String token = first("hall.internal.token", "HALL_INTERNAL_TOKEN");
        if (base == null || token == null || token.length() < 32) {
            LOG.severe("HALL_ROOM_CLOSE_CONFIG_MISSING roomId=" + roomId);
            return;
        }
        String traceId = UUID.randomUUID().toString();
        String requestId = "room-close-" + roomId;
        String json = "{\"requestId\":\"" + requestId + "\",\"reason\":\"AUTHORITY_REMOVED\"}";
        URI endpoint = URI.create(base.endsWith("/") ? base : base + "/")
                .resolve("internal/v1/hall/rooms/" + roomId + "/close");
        RuntimeException failure = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpRequest request = HttpRequest.newBuilder(endpoint)
                        .timeout(Duration.ofSeconds(3))
                        .header("Content-Type", "application/json")
                        .header("X-Aoo-Internal-Token", token)
                        .header("X-Trace-Id", traceId)
                        .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                        .build();
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() >= 200 && response.statusCode() < 300) return;
                failure = new IllegalStateException("Hall close rejected with HTTP " + response.statusCode());
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                failure = new IllegalStateException("Hall close interrupted", interrupted);
                break;
            } catch (Exception error) {
                failure = new IllegalStateException("Hall close unavailable", error);
            }
            if (attempt < MAX_ATTEMPTS) {
                try {
                    Thread.sleep(100L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        LOG.log(Level.SEVERE, "HALL_ROOM_CLOSE_FAILED roomId=" + roomId + " traceId=" + traceId, failure);
    }

    private static String first(String property, String... environmentKeys) {
        String value = System.getProperty(property);
        if (value != null && !value.isBlank()) return value.strip();
        for (String key : environmentKeys) {
            value = System.getenv(key);
            if (value != null && !value.isBlank()) return value.strip();
        }
        return null;
    }
}
