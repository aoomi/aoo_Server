package core.network.client2game.handler.scjymj;

import business.global.mj.scjymj.SCJYMJRoom;
import business.global.room.RoomMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.room.CBase_GetRoomInfo;

import java.io.IOException;

/**
 * 济宁房间记录
 *
 * @author Huaxing
 */
public class CSCJYMJRoomRecord extends PlayerHandler {


    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CBase_GetRoomInfo req = new Gson().fromJson(message, CBase_GetRoomInfo.class);
        long roomID = req.getRoomID();

        SCJYMJRoom room = (SCJYMJRoom) RoomMgr.getInstance().getRoom(roomID);
        if (null == room) {
            request.error(ErrorCode.NotAllow, "CSCJYMJRoomRecord not find room:" + roomID);
            return;
        }
        request.response(room.getRecord());
    }
}
