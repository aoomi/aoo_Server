package com.aoo.bcg.gamespi;

/** Server-read identity, room and version state used to bind an untrusted intent. */
public record AuthoritativeCommandContext(long roomId, int roundNo, String playVersion,
                                          String authenticatedUserId, int seatId,
                                          String connectionId, long connectionVersion,
                                          long fencingToken) {
    public AuthoritativeCommandContext {
        if (roomId <= 0 || roundNo < 0 || playVersion == null || playVersion.isBlank()
                || authenticatedUserId == null || authenticatedUserId.isBlank() || seatId < 0
                || connectionId == null || connectionId.isBlank() || connectionVersion <= 0
                || fencingToken <= 0) {
            throw new IllegalArgumentException("invalid authoritative command context");
        }
    }

    public GameCommandRequest bind(ClientCommandIntent intent, CommandSchema schema) {
        if (intent == null || schema == null) throw new IllegalArgumentException("intent and schema are required");
        schema.validate(intent.body());
        return new GameCommandRequest(intent.msgId(), intent.requestId(), intent.sequence(), roomId,
                roundNo, playVersion, authenticatedUserId, seatId, intent.body());
    }
}
