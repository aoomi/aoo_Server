package core.network.client2game.handler.room;

import business.player.Player;
import business.player.feature.PlayerRoom;
import com.ddm.server.common.utils.ErrorCodeException;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.room.CBase_EnterRoom;

import java.io.IOException;

/**
 * 获取房间游戏类型
 */
public class CBaseGetGameType extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CBase_EnterRoom req = new Gson().fromJson(message, CBase_EnterRoom.class);
        // 进入房间
        try {
            int gameType = player.getFeature(PlayerRoom.class).getGameTypeByRoomKey(req.getRoomKey());
            request.response(gameType);
        } catch (Exception e) {
            if (e instanceof ErrorCodeException) {
                request.error(((ErrorCodeException) e).getErrorCode(), e.getMessage());
            } else {
                request.error(ErrorCode.ErrorSysMsg, "系统报错");
            }
        }
    }
}
