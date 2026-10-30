package com.aoo.bcg.gamespi;

/** Opt-in authority implemented only by sessions that can produce the strict public room view. */
@FunctionalInterface
public interface CommonRoomPublicViewProvider {
    CommonRoomPublicView commonRoomPublicView(long viewerPlayerId);
}
