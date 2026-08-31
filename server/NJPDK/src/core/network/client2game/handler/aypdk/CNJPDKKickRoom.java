package core.network.client2game.handler.njpdk;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.room.CBase_KickRoom;

import java.io.IOException;

/**
 * 踢出房间
 *
 * @author Administrator
 */
public class CNJPDKKickRoom extends PlayerHandler {


    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CBase_KickRoom req = new Gson().fromJson(message, CBase_KickRoom.class);
        long roomID = req.roomID;
        int posIndex = req.posIndex;
        AbsBaseRoom room = RoomMgr.getInstance().getRoom(roomID);
        if (null == room) {
            request.error(ErrorCode.NotAllow, "CPDKKickRoom not find room:" + roomID);
            return;
        }
        SData_Result result = room.kickOut(player.getPid(), posIndex);
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response();
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
