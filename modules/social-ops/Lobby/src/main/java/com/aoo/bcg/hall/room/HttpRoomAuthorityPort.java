package com.aoo.bcg.hall.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aoo.bcg.lobby.LobbyHttpRoomAuthorityPort;
import com.aoo.bcg.lobby.LobbyRoomAuthorityRejected;
import java.net.URI;
import java.util.Map;

/** Compatibility facade; RoomOrchestrator authority calls are owned by Lobby. */
public class HttpRoomAuthorityPort extends LobbyHttpRoomAuthorityPort implements RoomMembershipAuthorityPort {
    public HttpRoomAuthorityPort(URI base, String token, ObjectMapper json) {
        super(base, token, json);
    }

    /** Preserve the frozen Hall exception type while Lobby owns the call. */
    @Override
    public Map<String, Object> leave(long roomId, long accountId, String playVersion,
            String requestId, String traceId, Map<String, Object> completeIdentity) {
        try {
            return super.leave(roomId, accountId, playVersion, requestId, traceId, completeIdentity);
        } catch (LobbyRoomAuthorityRejected rejected) {
            throw new RoomAuthorityRejected(rejected.status(), rejected.code(), rejected.getMessage());
        }
    }
}
