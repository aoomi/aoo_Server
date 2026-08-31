package com.aoo.bcg.gateway;
import java.util.Map;
public record WebSocketFrame(String protocolVersion, String msgId, String kind, String requestId, long seq,
                             String traceId, String roomId, int roundNo, String playVersion,
                             long timestamp, Map<String, Object> body) {
    public WebSocketFrame(String msgId, String requestId, long seq, String roomId, int roundNo,
                          String playVersion, long timestamp, Map<String, Object> body) {
        this("2.0", msgId, "req", requestId, seq, requestId, roomId, roundNo, playVersion, timestamp, body);
    }

    public WebSocketFrame {
        if (!"2.0".equals(protocolVersion) || !"req".equals(kind) || traceId == null || traceId.isBlank()) throw new IllegalArgumentException("invalid websocket envelope");
        if (msgId == null || !msgId.matches("[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+") || requestId == null || requestId.isBlank() || requestId.length() > 128 || seq <= 0 || roundNo < 0 || timestamp <= 0) throw new IllegalArgumentException("invalid websocket frame");
        boolean nonRoom = isNonRoomMessage(msgId);
        if (!nonRoom && (roomId == null || roomId.isBlank())) throw new IllegalArgumentException("roomId is required");
        if (!nonRoom && (playVersion == null || playVersion.isBlank())) throw new IllegalArgumentException("playVersion is required");
        body = Map.copyOf(body == null ? Map.of() : body);
        ProtocolValuePolicy.validate(body);
    }

    public static boolean isNonRoomMessage(String msgId) {
        return "gateway.heartbeat".equals(msgId) || "account.session_dispatch".equals(msgId)
                || "hall.dispatch".equals(msgId) || "club.dispatch".equals(msgId);
    }
}
