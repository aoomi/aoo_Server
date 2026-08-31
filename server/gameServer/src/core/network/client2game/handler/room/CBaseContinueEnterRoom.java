package core.network.client2game.handler.room;

import business.player.Player;
import business.player.feature.PlayerRoom;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import java.io.IOException;

/** Shared reconnect/continue-entry command. */
@Deprecated
public abstract class CBaseContinueEnterRoom extends PlayerHandler {
    @Override public final void handle(Player player, WebSocketRequest request, String message) throws IOException {
        business.global.protocol.LegacyProtocolUsage.record("room.continue_enter");
        SData_Result result = player.getFeature(PlayerRoom.class).continueFindAndEnter();
        if (ErrorCode.Success.equals(result.getCode())) request.response(result.getData()); else request.error(result.getCode(), result.getMsg());
    }
}
