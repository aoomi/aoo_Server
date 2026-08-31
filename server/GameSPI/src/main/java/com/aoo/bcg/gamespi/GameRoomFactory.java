package com.aoo.bcg.gamespi;

public interface GameRoomFactory {
    GameRoomHandle create(RoomCreationContext context);
}
