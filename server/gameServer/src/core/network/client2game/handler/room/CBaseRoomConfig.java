package core.network.client2game.handler.room;

import business.player.Player;
import business.player.feature.PlayerRoom;
import com.ddm.server.common.Config;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;

import java.io.IOException;

public class CBaseRoomConfig extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        //共享
        if (Config.isShare()) {
            SData_Result result = player.getFeature(PlayerRoom.class).onRoomConfigShare();
            if (ErrorCode.Success.equals(result.getCode())) {
                request.response(result.getData());
            } else {
                request.error(result.getCode(), result.getMsg());
            }
        } else {
            SData_Result result = player.getFeature(PlayerRoom.class).onRoomConfig();
            if (ErrorCode.Success.equals(result.getCode())) {
                request.response(result.getData());
            } else {
                request.error(result.getCode(), result.getMsg());
            }
        }
    }
}
