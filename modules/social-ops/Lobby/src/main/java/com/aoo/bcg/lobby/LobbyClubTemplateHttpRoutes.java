package com.aoo.bcg.lobby;

import com.aoo.bcg.hall.http.HallError;
import com.aoo.bcg.common.error.ErrorCodeCatalog;
import com.aoo.bcg.lobby.JdbcLobbyRepository;
import com.aoo.bcg.lobby.LobbyRoomCreateSaga;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.util.*;

/** Lobby-owned materialization of a visible Club template into a real room. */
public final class LobbyClubTemplateHttpRoutes {
    private static final Set<String> TEMPLATE_POLICY_FIELDS = Set.of(
            "baseScore", "roomSportsType", "roomSportsEveryoneConsume", "percentage", "prizePool",
            "bigWinnerConsumeList", "rule", "roomSportsThreshold", "JoinGamePoint", "autoDismiss");

    private LobbyClubTemplateHttpRoutes() {}

    @FunctionalInterface
    public interface TemplateResolver {
        Map<String, Object> resolve(long actor, long clubId, long gameIndex);
    }

    public static void mount(HttpServer server, JdbcLobbyRepository repository, LobbyRequestAuthenticator auth,
            ObjectMapper json, LobbyRoomCreateSaga saga, TemplateResolver resolver) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(repository, "repository");
        Objects.requireNonNull(auth, "auth");
        Objects.requireNonNull(json, "json");
        Objects.requireNonNull(saga, "roomCreateSaga");
        Objects.requireNonNull(resolver, "templateResolver");
        server.createContext("/api/v2/hall/club-template-rooms",
                exchange -> handle(exchange, repository, auth, json, saga, resolver));
    }

    private static void handle(HttpExchange exchange, JdbcLobbyRepository repository, LobbyRequestAuthenticator auth,
            ObjectMapper json, LobbyRoomCreateSaga saga, TemplateResolver resolver) throws IOException {
        String trace = Optional.ofNullable(exchange.getRequestHeaders().getFirst("X-Trace-Id"))
                .filter(value -> !value.isBlank()).orElseGet(() -> UUID.randomUUID().toString());
        try {
            if (!"POST".equals(exchange.getRequestMethod())) {
                throw new HallError(405, "HALL_METHOD_NOT_ALLOWED", "only POST is supported");
            }
            long actor = auth.authenticate(exchange.getRequestHeaders().getFirst("Authorization"));
            String requestId = key(exchange);
            requireKey(requestId);
            Map<String, Object> request = body(exchange, json);
            long clubId = positive(request, "clubId");
            long gameIndex = positive(request, "roomKey");
            Map<String, Object> template = resolver.resolve(actor, clubId, gameIndex);
            String gameCode = text(template, "gameCode");
            Map<String, Object> game = repository.catalog("ALL", "3.8.8").stream()
                    .filter(candidate -> gameCode.equalsIgnoreCase(String.valueOf(candidate.get("gameCode"))))
                    .findFirst().orElseThrow(() -> new HallError(503, "HALL_GAME_UNAVAILABLE", "club template game is unavailable"));
            long gameId = positive(game, "gameId");
            String playVersion = text(game, "playVersion");
            Map<String, Object> rules = gameplayRules(repository.configuration(gameId, playVersion, "3.8.8"), template.get("rules"));
            long roomId = repository.allocateRoomId(actor, requestId);
            Map<String, Object> create = new LinkedHashMap<>();
            create.put("roomId", roomId);
            create.put("gameId", gameId);
            create.put("playVersion", playVersion);
            create.put("clientVersion", "3.8.8");
            create.put("ruleRevision", repository.currentRuleRevision(gameId, playVersion, "3.8.8"));
            create.put("stateVersion", 1L);
            create.put("rules", rules);
            create.put("scope", Map.of("type", "CLUB", "clubId", clubId,
                    "templateCode", text(template, "templateCode")));
            create.put("_traceId", trace);
            // LobbyRoomCreateSaga registers the creator as seat 0 atomically. There is no
            // second Club/WebSocket join step and therefore no duplicate lifecycle.
            Map<String, Object> result = new LinkedHashMap<>(saga.createClubTemplate(actor, requestId, create));
            result.put("roomId", roomId);
            result.put("roomKey", roomId);
            reply(exchange, 200, Map.of("code", "OK", "data", Map.copyOf(result), "traceId", trace), json);
        } catch (HallError error) {
            reply(exchange, error.status(), Map.of("code", error.code(), "message", error.getMessage(), "traceId", trace), json);
        } catch (SecurityException error) {
            reply(exchange, 403, Map.of("code", "HALL_CLUB_ACCESS_DENIED", "message", error.getMessage(), "traceId", trace), json);
        } catch (NumberFormatException error) {
            reply(exchange, 400, Map.of("code", "HALL_INVALID_REQUEST", "message", "numeric field is invalid", "traceId", trace), json);
        } catch (Exception error) {
            Throwable root = error;
            while (root.getCause() != null) root = root.getCause();
            System.err.println("hall club template request failed trace=" + trace + " cause="
                    + root.getClass().getSimpleName() + ":" + String.valueOf(root.getMessage()));
            reply(exchange, 500, Map.of("code", "HALL_INTERNAL_ERROR", "canonicalCode", ErrorCodeCatalog.classify(error, "UPSTREAM_UNAVAILABLE"), "message", String.valueOf(error.getMessage()), "traceId", trace), json);
        }
    }

    private static Map<String, Object> gameplayRules(Map<String, Object> configuration, Object rawRules) {
        if (!(rawRules instanceof Map<?, ?> source)) return Map.of();
        Set<String> allowed = new HashSet<>();
        Object ui = configuration.get("ui");
        if (ui instanceof Map<?, ?> uiObject && uiObject.get("fields") instanceof List<?> fields) {
            for (Object rawField : fields) {
                if (rawField instanceof Map<?, ?> field) {
                    Object key = field.containsKey("key") ? field.get("key") : field.get("fieldId");
                    if (key != null && !String.valueOf(key).isBlank()) allowed.add(String.valueOf(key));
                }
            }
        }
        Map<String, Object> rules = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String name = String.valueOf(key);
            if (allowed.contains(name) || TEMPLATE_POLICY_FIELDS.contains(name)) rules.put(name, value);
        });
        return Map.copyOf(rules);
    }

    private static Map<String, Object> body(HttpExchange exchange, ObjectMapper json) throws IOException {
        byte[] bytes = exchange.getRequestBody().readAllBytes();
        if (bytes.length == 0) return Map.of();
        if (bytes.length > 1_048_576) throw new HallError(400, "HALL_INVALID_REQUEST", "request body too large");
        return json.readValue(bytes, new TypeReference<>() {});
    }

    private static long positive(Map<String, Object> values, String key) {
        Object raw = values.get(key);
        if (raw instanceof Number number && number.longValue() > 0) return number.longValue();
        try { long value = Long.parseLong(String.valueOf(raw)); if (value > 0) return value; }
        catch (RuntimeException ignored) {}
        throw new HallError(400, "HALL_INVALID_REQUEST", key + " is required");
    }

    private static String text(Map<String, Object> values, String key) {
        String value = String.valueOf(values.getOrDefault(key, "")).strip();
        if (value.isBlank()) throw new HallError(400, "HALL_INVALID_REQUEST", key + " is required");
        return value;
    }

    private static String key(HttpExchange exchange) {
        String value = exchange.getRequestHeaders().getFirst("X-Idempotency-Key");
        return value == null ? exchange.getRequestHeaders().getFirst("Idempotency-Key") : value;
    }

    private static void requireKey(String value) {
        if (value == null || !value.matches("[A-Za-z0-9_.:-]{16,128}"))
            throw new HallError(400, "HALL_INVALID_REQUEST", "idempotency key is required");
    }

    private static void reply(HttpExchange exchange, int status, Object value, ObjectMapper json) throws IOException {
        byte[] bytes = json.writeValueAsBytes(value);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
