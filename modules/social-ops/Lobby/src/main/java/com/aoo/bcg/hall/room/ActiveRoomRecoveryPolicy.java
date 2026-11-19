package com.aoo.bcg.hall.room;

import java.util.Map;

/** Compatibility facade; active-room recovery policy is owned by Lobby. */
final class ActiveRoomRecoveryPolicy {
    private ActiveRoomRecoveryPolicy() {}
    static boolean isRecoverable(String hallState, String authorityState, Map<String,Object> snapshot) {
        return com.aoo.bcg.lobby.LobbyActiveRoomRecoveryPolicy.isRecoverable(hallState, authorityState, snapshot);
    }
}
