package com.aoo.bcg.gamespi;

import java.util.Map;
import java.util.LinkedHashMap;
import com.aoo.bcg.gamespi.time.OperationDeadline;

public record GameCommandResult(String msgId, String requestId, CommandPayload body,
                                long serverTimeEpochMillis, Map<String, Object> operationDeadline,
                                CommittedCommandBinding committedBinding) {
    public GameCommandResult(String msgId, String requestId, CommandPayload body,
                             long serverTimeEpochMillis, Map<String,Object> operationDeadline) {
        this(msgId, requestId, body, serverTimeEpochMillis, operationDeadline, null);
    }
    public GameCommandResult withCommittedBinding(CommittedCommandBinding binding) {
        java.util.Objects.requireNonNull(binding);
        if (!requestId.equals(binding.requestId())
                || (committedBinding != null && !committedBinding.equals(binding)))
            throw new IllegalArgumentException("committed command binding conflict");
        return new GameCommandResult(msgId, requestId, body, serverTimeEpochMillis, operationDeadline, binding);
    }
    public GameCommandResult(String msgId, String requestId, Map<String, Object> body) {
        this(msgId, requestId, CommandPayload.copyOf(body), 0L, Map.of());
    }
    public GameCommandResult {
        if (msgId == null || msgId.isBlank() || requestId == null || requestId.isBlank())
            throw new IllegalArgumentException("invalid game command result");
        if (committedBinding != null && (!requestId.equals(committedBinding.requestId())))
            throw new IllegalArgumentException("committed command binding scope conflict");
        body = body == null ? CommandPayload.empty() : body;
        operationDeadline = Map.copyOf(operationDeadline == null ? Map.of() : operationDeadline);
        if (serverTimeEpochMillis < 0) throw new IllegalArgumentException("invalid server time");
    }
    public GameCommandResult withTiming(long serverTimeEpochMillis, OperationDeadline deadline) {
        return new GameCommandResult(msgId, requestId, body, serverTimeEpochMillis, deadline.toMap(), committedBinding);
    }
    public GameCommandResult withAuthorityMetadata(long stateVersion, long serverSequence) {
        if (stateVersion < 0 || serverSequence <= 0) throw new IllegalArgumentException("invalid authority metadata");
        Map<String,Object> output = new LinkedHashMap<>(body.asMap());
        output.put("stateVersion", stateVersion);
        output.put("serverSeq", serverSequence);
        return new GameCommandResult(msgId, requestId, CommandPayload.copyOf(output), serverTimeEpochMillis, operationDeadline, committedBinding);
    }
}
