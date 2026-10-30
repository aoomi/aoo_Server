package core.network.client2game.handler.scjymj;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.room.CBase_Trusteeship;

import java.io.IOException;

/**
 * 玩家是否托管
 *
 * @author Huaxing
 */
public class CSCJYMJTrusteeship extends PlayerHandler {

    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CBase_Trusteeship req = new Gson().fromJson(message, CBase_Trusteeship.class);
        long roomID = req.roomID;
        boolean trusteeship = req.trusteeship;

        AbsBaseRoom room = RoomMgr.getInstance().getRoom(roomID);
        if (null == room) {
            request.error(ErrorCode.NotAllow, "CSCJYMJTrusteeship not find room:" + roomID);
            return;
        }
        SData_Result result = room.opRoomTrusteeship(player.getPid(), trusteeship);
        if (ErrorCode.Success.equals(result.getCode())) {
            request.response();
        } else {
            request.error(result.getCode(), result.getMsg());
        }
    }

}
