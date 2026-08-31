package core.network.client2game.handler.njpdk;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.room.CBase_ShowLeave;

import java.io.IOException;

/**
 * 操作是否显示离开
 *
 * @author Administrator
 */
public class CNJPDKShowLeave extends PlayerHandler {


    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CBase_ShowLeave req = new Gson().fromJson(message, CBase_ShowLeave.class);
        long roomID = req.getRoomID();
        AbsBaseRoom room = RoomMgr.getInstance().getRoom(roomID);
        if (null == room) {
            request.error(ErrorCode.NotAllow, "CPDKShowLeave not find room:" + roomID);
            return;
        }
        SData_Result result = room.opShowLeave(player.getPid(), req.isShowLeave());
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response();
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }
}
