package core.network.client2game.handler.scjymj;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.room.CBase_RoomID;

import java.io.IOException;

/**
 * 开始游戏
 *
 * @author Administrator
 */
public class CSCJYMJStartGame extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CBase_RoomID req = new Gson().fromJson(message, CBase_RoomID.class);

        AbsBaseRoom room = RoomMgr.getInstance().getRoom(req.getRoomID());
        if (null == room) {
            request.error(ErrorCode.NotAllow, "CSCJYMJStartGame not find room:" + req.getRoomID());
            return;
        }

        // 开始游戏
        SData_Result result = room.startGame(player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response();
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
