package core.network.client2game.handler.scjymj;

import business.global.mj.scjymj.SCJYMJRoom;
import business.global.room.RoomMgr;
import business.player.Player;
import business.scjymj.c2s.cclass.SCJYMJResults;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.room.CBase_GetRoomInfo;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class CSCJYMJRoomXRecord extends PlayerHandler {


    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CBase_GetRoomInfo req = new Gson().fromJson(message, CBase_GetRoomInfo.class);
        long roomID = req.getRoomID();

        SCJYMJRoom room = (SCJYMJRoom) RoomMgr.getInstance().getRoom(roomID);
        if (null == room) {
            request.error(ErrorCode.NotAllow, "CSCJYMJRoomEndResult not find room:" + roomID);
            return;
        }
        List<SCJYMJResults> results = room.getRoomPosMgr().getPosList().stream().filter(k -> k.getPid() > 0L && k.isPlayTheGame())
                .map(k -> (SCJYMJResults) k.getResults()).filter(n -> n != null).collect(Collectors.toList());
        request.response(results);
    }
}
