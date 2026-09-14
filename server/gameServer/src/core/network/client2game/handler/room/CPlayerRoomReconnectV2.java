package core.network.client2game.handler.room;

import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;

/** Retired legacy reconnect entry. Production recovery is protocol.v2.dispatch -> room.reconnect only. */
public final class CPlayerRoomReconnectV2 extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        request.error(ErrorCode.NotAllow, "legacy reconnect retired; use authenticated protocol V2 room.reconnect");
    }
}
