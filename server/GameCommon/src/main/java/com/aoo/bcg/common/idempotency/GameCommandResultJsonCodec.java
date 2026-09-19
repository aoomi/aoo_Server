package com.aoo.bcg.common.idempotency;

import com.aoo.bcg.common.serialization.DomainJsonDecoder;
import com.aoo.bcg.gamespi.CommandPayload;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

/** Central compatibility codec for durable command results across the Map-to-CommandPayload migration. */
final class GameCommandResultJsonCodec {
    private static final TypeReference<Map<String, Object>> DOCUMENT = new TypeReference<>() {};
    private GameCommandResultJsonCodec() {}

    static GameCommandResult decode(ObjectMapper json, String stored) {
        try {
            Map<String, Object> document = json.convertValue(json.readTree(stored), DOCUMENT);
            DomainJsonDecoder.requireDocument(document);
            String msgId = requiredText(document, "msgId");
            String requestId = requiredText(document, "requestId");
            Map<String, Object> body = document.get("body") instanceof Map<?, ?> raw ? stringMap(raw) : Map.of();
            long serverTime = document.get("serverTimeEpochMillis") instanceof Number number ? number.longValue() : 0L;
            Map<String, Object> deadline = document.get("operationDeadline") instanceof Map<?, ?> raw ? stringMap(raw) : Map.of();
            return new GameCommandResult(msgId, requestId, CommandPayload.copyOf(body), serverTime, deadline);
        } catch (com.fasterxml.jackson.core.JsonProcessingException error) {
            throw new IllegalArgumentException("invalid game command result JSON", error);
        }
    }

    static String encode(ObjectMapper json, GameCommandResult result) throws com.fasterxml.jackson.core.JsonProcessingException {
        Map<String, Object> document = new LinkedHashMap<>();
        document.put("msgId", result.msgId()); document.put("requestId", result.requestId());
        document.put("body", result.body().asMap());
        document.put("serverTimeEpochMillis", result.serverTimeEpochMillis());
        document.put("operationDeadline", result.operationDeadline());
        return json.writeValueAsString(document);
    }

    private static String requiredText(Map<String, Object> document, String key) {
        Object value = document.get(key);
        if (!(value instanceof String text) || text.isBlank()) throw new IllegalArgumentException("missing " + key);
        return text;
    }
    private static Map<String, Object> stringMap(Map<?, ?> raw) {
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, value) -> result.put(String.valueOf(key), value));
        DomainJsonDecoder.requireDocument(result);
        return result;
    }
}
