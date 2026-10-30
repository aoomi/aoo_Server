package com.aoo.bcg.gamespi;

/** Keeps old runtime access available while unified commands use a separate authority. */
public interface LegacyCompatibleRoom {
    Object legacyRoom();
    AuthoritativeGameSession authoritativeSession();
}
