package com.aoo.bcg.gamespi;

import java.util.Map;

public record GameCommandRequest(String msgId, String requestId, long sequence, long roomId,
                                 int roundNo, String playVersion, String authenticatedUserId,
                                 int seatId, CommandPayload body) {
    public GameCommandRequest(String msgId, String requestId, long sequence, long roomId,
                              int roundNo, String playVersion, String authenticatedUserId,
                              int seatId, Map<String, Object> body) {
        this(msgId, requestId, sequence, roomId, roundNo, playVersion, authenticatedUserId,
                seatId, CommandPayload.copyOf(body));
    }
    public GameCommandRequest {
        if (msgId == null || msgId.isBlank() || requestId == null || requestId.isBlank()
                || sequence <= 0 || roomId <= 0 || roundNo < 0 || playVersion == null
                || playVersion.isBlank() || authenticatedUserId == null
                || authenticatedUserId.isBlank() || seatId < 0) {
            throw new IllegalArgumentException("invalid game command request");
        }
        body = body == null ? CommandPayload.empty() : body;
    }

    public PlayerSeatIdentity playerSeatIdentity() {
        return new PlayerSeatIdentity(authenticatedUserId, seatId);
    }
}
