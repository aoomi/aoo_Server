package com.ddm.server.protocol.v2;

public final class ProtocolValidator {
    private ProtocolValidator() {}

    public static void validate(ProtocolEnvelope message) {
        if (message == null) throw new IllegalArgumentException("Message is required");
        if (!"2.0".equals(message.protocolVersion)) throw new IllegalArgumentException("Unsupported protocolVersion");
        required(message.msgId, "msgId");
        required(message.kind, "kind");
        required(message.requestId, "requestId");
        required(message.traceId, "traceId");
        if (message.seq <= 0) throw new IllegalArgumentException("seq must be positive");
        if (message.timestamp <= 0) throw new IllegalArgumentException("timestamp must be positive");
    }

    private static void required(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
    }
}
