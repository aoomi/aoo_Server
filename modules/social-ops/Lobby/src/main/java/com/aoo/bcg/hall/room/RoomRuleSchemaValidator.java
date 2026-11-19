package com.aoo.bcg.hall.room;

import java.util.Map;

/** Compatibility facade; published rule validation is owned by Lobby. */
final class RoomRuleSchemaValidator {
    private RoomRuleSchemaValidator() {}
    static Map<String,Object> validate(Object schema, Object submitted) {
        return com.aoo.bcg.lobby.LobbyRoomRuleSchemaValidator.validate(schema, submitted);
    }
    static Map<String,Object> validate(Object schema, Object submitted, long gameId, String playVersion) {
        return com.aoo.bcg.lobby.LobbyRoomRuleSchemaValidator.validate(schema, submitted, gameId, playVersion);
    }
}
