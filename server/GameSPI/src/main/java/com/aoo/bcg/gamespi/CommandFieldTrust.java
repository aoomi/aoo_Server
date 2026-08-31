package com.aoo.bcg.gamespi;

/** Declares whether a command field is an intent or must be derived by the server. */
public enum CommandFieldTrust {
    CLIENT_INTENT,
    SERVER_IDENTITY,
    SERVER_ROOM,
    SERVER_RULE,
    SERVER_STATE,
    SERVER_TIME,
    SERVER_RANDOM,
    SERVER_DERIVED
}
