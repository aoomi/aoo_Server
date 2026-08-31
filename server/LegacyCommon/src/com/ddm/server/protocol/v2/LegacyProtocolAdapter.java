package com.ddm.server.protocol.v2;

import java.util.Map;

/** The only allowed bridge to the signed-short V1 wire protocol. */
public final class LegacyProtocolAdapter {
    private LegacyProtocolAdapter() {}

    public static ProtocolEnvelope fromV1(String event, short sequence, short errorCode,
                                          Map<String, Object> body, String requestId) {
        ProtocolEnvelope value = new ProtocolEnvelope();
        value.msgId = event;
        value.kind = "req";
        value.requestId = requestId;
        value.seq = Short.toUnsignedLong(sequence);
        value.timestamp = System.currentTimeMillis();
        value.traceId = requestId;
        value.code = (int) errorCode;
        value.body = body;
        return value;
    }
}
