package core.network.client2game.handler.scjymj;

import business.global.mj.scjymj.SCJYMJRoom;
import business.global.room.RoomMgr;
import business.player.Player;
import business.scjymj.c2s.iclass.CSCJYMJ_OpPiao;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;

/**
 * 打牌
 *
 * @author Huaxing
 */
public class CSCJYMJOpPiao extends PlayerHandler {


    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CSCJYMJ_OpPiao req = new Gson().fromJson(message, CSCJYMJ_OpPiao.class);
        long roomID = req.roomID;
        SCJYMJRoom room = (SCJYMJRoom) RoomMgr.getInstance().getRoom(roomID);
        if (null == room) {
            request.error(ErrorCode.NotAllow, "CSCJYMJOpCard not find room:" + roomID);
            return;
        }
        room.opPiao(request, player.getId(), req.value);
    }
}
