package com.aoo.bcg.hall.room;

import com.aoo.bcg.lobby.LobbyRoomCreateRecoveryService;

/** Compatibility facade; recovery scheduling is owned by Lobby. */
public class RoomCreateRecoveryService extends LobbyRoomCreateRecoveryService {
    public RoomCreateRecoveryService(RoomCreateSaga saga, int batch) {
        super(saga, batch);
    }
}
