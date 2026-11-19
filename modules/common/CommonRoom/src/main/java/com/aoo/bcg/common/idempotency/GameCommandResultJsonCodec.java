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
        return decode(json, stored, null, 1);
    }
    static GameCommandResult decode(ObjectMapper json, String stored, IdempotencyKey key, int schema) {
        try {
            Map<String, Object> document = json.convertValue(json.readTree(stored), DOCUMENT);
            DomainJsonDecoder.requireDocument(document);
            String msgId = requiredText(document, "msgId");
            String requestId = requiredText(document, "requestId");
            Map<String, Object> body = document.get("body") instanceof Map<?, ?> raw ? stringMap(raw) : Map.of();
            long serverTime = document.get("serverTimeEpochMillis") instanceof Number number ? number.longValue() : 0L;
            Map<String, Object> deadline = document.get("operationDeadline") instanceof Map<?, ?> raw ? stringMap(raw) : Map.of();
            GameCommandResult result = new GameCommandResult(msgId, requestId, CommandPayload.copyOf(body), serverTime, deadline);
            if (schema == 1) return result;
            if (schema != 2) throw new IllegalArgumentException("unsupported command result schema");
            return result.withCommittedBinding(decodeCommittedBinding(json, json.readTree(stored).get("committedBinding"), key));
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
        if (result.committedBinding() != null) document.put("committedBinding", result.committedBinding());
        return json.writeValueAsString(document);
    }

    static com.aoo.bcg.gamespi.CommittedCommandBinding decodeCommittedBinding(ObjectMapper json,
            com.fasterxml.jackson.databind.JsonNode node, IdempotencyKey key) {
        if (node == null || !node.isObject() || key == null)
            throw new IllegalArgumentException("missing committed binding or scope");
        var binding = new com.aoo.bcg.gamespi.CommittedCommandBinding(
                exactInt(node,"schemaVersion"), text(node,"userId"), text(node,"operation"),
                exactLong(node,"roomId"), exactInt(node,"roundNo"), text(node,"requestId"),
                text(node,"roomInstanceId"), exactLong(node,"lifecycleEpoch"),
                exactLong(node,"fencingToken"), exactLong(node,"stateVersion"));
        binding.requireScope(key.userId(),key.operation(),key.roomId(),key.roundNo(),key.requestId());
        return binding;
    }
    private static String text(com.fasterxml.jackson.databind.JsonNode node, String field) {
        var value=node.get(field);
        if(value==null || !value.isTextual()) throw new IllegalArgumentException("invalid binding " + field);
        return value.textValue();
    }
    private static long exactLong(com.fasterxml.jackson.databind.JsonNode node, String field) {
        var value=node.get(field);
        if(value==null || !value.isIntegralNumber() || !value.canConvertToLong())
            throw new IllegalArgumentException("invalid binding " + field);
        return value.longValue();
    }
    private static int exactInt(com.fasterxml.jackson.databind.JsonNode node, String field) {
        long value=exactLong(node,field);
        if(value<Integer.MIN_VALUE || value>Integer.MAX_VALUE) throw new IllegalArgumentException("invalid binding " + field);
        return (int)value;
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
