package com.aoo.bcg.hall.room;

/** Frozen Hall exception facade forwarded from the Lobby authority port. */
public class RoomAuthorityRejected extends com.aoo.bcg.lobby.LobbyRoomAuthorityRejected {
    public RoomAuthorityRejected(int status, String code, String message) {
        super(status, code, message);
    }
}
