package com.aoo.bcg.gamespi;

import java.util.Map;
import java.util.LinkedHashMap;
import com.aoo.bcg.gamespi.time.OperationDeadline;

public record GameCommandResult(String msgId, String requestId, CommandPayload body,
                                long serverTimeEpochMillis, Map<String, Object> operationDeadline) {
    public GameCommandResult(String msgId, String requestId, Map<String, Object> body) {
        this(msgId, requestId, CommandPayload.copyOf(body), 0L, Map.of());
    }
    public GameCommandResult {
        if (msgId == null || msgId.isBlank() || requestId == null || requestId.isBlank())
            throw new IllegalArgumentException("invalid game command result");
        body = body == null ? CommandPayload.empty() : body;
        operationDeadline = Map.copyOf(operationDeadline == null ? Map.of() : operationDeadline);
        if (serverTimeEpochMillis < 0) throw new IllegalArgumentException("invalid server time");
    }
    public GameCommandResult withTiming(long serverTimeEpochMillis, OperationDeadline deadline) {
        return new GameCommandResult(msgId, requestId, body, serverTimeEpochMillis, deadline.toMap());
    }
    public GameCommandResult withAuthorityMetadata(long stateVersion, long serverSequence) {
        if (stateVersion < 0 || serverSequence <= 0) throw new IllegalArgumentException("invalid authority metadata");
        Map<String,Object> output = new LinkedHashMap<>(body.asMap());
        output.put("stateVersion", stateVersion);
        output.put("serverSeq", serverSequence);
        return new GameCommandResult(msgId, requestId, CommandPayload.copyOf(output), serverTimeEpochMillis, operationDeadline);
    }
}
