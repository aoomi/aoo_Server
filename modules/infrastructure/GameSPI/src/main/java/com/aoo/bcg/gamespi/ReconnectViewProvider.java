package com.aoo.bcg.gamespi;

public interface ReconnectViewProvider<T> {
    T buildFor(long viewerPlayerId, GameRoomHandle room);
}
