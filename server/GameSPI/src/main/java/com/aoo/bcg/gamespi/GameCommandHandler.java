package com.aoo.bcg.gamespi;

@FunctionalInterface
public interface GameCommandHandler {
    GameCommandResult handle(GameRoomHandle room, GameCommandRequest request);
}
