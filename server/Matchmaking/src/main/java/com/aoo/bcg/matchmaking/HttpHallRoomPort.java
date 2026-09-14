package com.aoo.bcg.matchmaking;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Internal authenticated adapter to the single Hall RoomCreateSaga entry. */
public final class HttpHallRoomPort implements MatchmakingPorts.HallRoomPort {
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    private final URI endpoint;
    private final String token;
    private final ObjectMapper json;

    public HttpHallRoomPort(URI hallBase, String token, ObjectMapper json) {
        this.endpoint = Objects.requireNonNull(hallBase).resolve("/internal/v1/hall/rooms");
        if (token == null || token.length() < 32) throw new IllegalArgumentException("hall token must contain at least 32 characters");
        this.token = token;
        this.json = Objects.requireNonNull(json);
    }

    @Override
    public Map<String, Object> create(long accountId, String idempotencyKey, Map<String, Object> request) {
        try {
            if (request.containsKey("roomId") || request.containsKey("regionCode")) {
                throw new IllegalArgumentException("matchmaking cannot submit room authority or region routing fields");
            }
            Map<String, Object> body = new LinkedHashMap<>(request);
            body.put("accountId", accountId);
            body.put("requestId", idempotencyKey);
            HttpRequest call = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json")
                    .header("X-Aoo-Internal-Token", token)
                    .header("X-Trace-Id", idempotencyKey)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(json.writeValueAsBytes(body)))
                    .build();
            HttpResponse<byte[]> response = http.send(call, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() / 100 != 2) throw new IllegalStateException("Hall room saga rejected matchmaking request: " + response.statusCode());
            Map<String, Object> envelope = json.readValue(response.body(), new TypeReference<>() {});
            if (!(envelope.get("data") instanceof Map<?, ?> raw)) throw new IllegalStateException("Hall room saga response is missing data");
            @SuppressWarnings("unchecked") Map<String, Object> data = (Map<String, Object>) raw;
            return Map.copyOf(data);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Hall room saga call interrupted", interrupted);
        } catch (RuntimeException runtime) {
            throw runtime;
        } catch (Exception failure) {
            throw new IllegalStateException("Hall room saga unavailable", failure);
        }
    }
}
