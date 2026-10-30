package com.aoo.bcg.gateway;

import com.aoo.bcg.gamespi.GameRoomHandle;

@FunctionalInterface
public interface GameRoomResolver {
    GameRoomHandle require(long roomId);
}
