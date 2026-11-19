package com.aoo.bcg.gamespi;

public interface GameProtocolHandler<I, O> {
    String operation();
    O handle(long authenticatedPlayerId, I intent);
}
