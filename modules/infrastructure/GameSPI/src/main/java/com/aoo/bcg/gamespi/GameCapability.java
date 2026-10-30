package com.aoo.bcg.gamespi;

/** Capabilities that every production game must account for explicitly. */
public enum GameCapability {
    LIFECYCLE,
    AUTHORITATIVE_COMMANDS,
    COMMAND_SCHEMA,
    RULES,
    SCORING,
    SETTLEMENT,
    RECONNECT,
    REPLAY,
    TRUSTEESHIP,
    PROTOCOL,
    PLAYER_PERSPECTIVE,
    RANDOMNESS,
    STATE_MIGRATION,
    INVARIANTS,
    SERIAL_EXECUTION,
    VERSION_LOCK,
    EVENT_JOURNAL,
    /** The provider explicitly permits a player-requested pre-round shuffle. */
    ROOM_SHUFFLE
}
