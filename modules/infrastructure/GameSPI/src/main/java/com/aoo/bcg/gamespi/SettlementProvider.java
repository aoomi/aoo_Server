package com.aoo.bcg.gamespi;

@FunctionalInterface
public interface SettlementProvider {
    SettlementPayload settle(GameRoomHandle room, int roundNo);
}
