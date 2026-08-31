package core.network.client2game.handler.game;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.room.CBase_GetRoomInfo;

import java.io.IOException;

/**
 * 获取所有人的定位信息
 */

public class CGetAllLocation extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CBase_GetRoomInfo req = new Gson().fromJson(message, CBase_GetRoomInfo.class);
        AbsBaseRoom room = RoomMgr.getInstance().getRoom(req.getRoomID());
        // 检查房间是否存在
        if (null == room) {
            request.error(ErrorCode.NotFind_Room, "CGetAllLocation null == room");
            return;
        }
        SData_Result result = room.opGetAllLoaction(player.getPid());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response(result.getData());
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }

}
