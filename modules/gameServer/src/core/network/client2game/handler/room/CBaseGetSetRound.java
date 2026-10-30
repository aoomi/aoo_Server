package core.network.client2game.handler.room;

import business.global.mj.template.MJTemplateRoom;
import business.global.room.RoomMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.mj.CMJ_OpCard;

import java.io.IOException;

public class CBaseGetSetRound extends PlayerHandler {


    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        final CMJ_OpCard req = new Gson().fromJson(message, CMJ_OpCard.class);
        long roomID = req.roomID;
        MJTemplateRoom room = (MJTemplateRoom) RoomMgr.getInstance().getRoom(roomID);
        if (null == room) {
            request.error(ErrorCode.NotAllow, "CDEMOMJGetSetRound not find room:" + roomID);
            return;
        }
        SData_Result result = room.setRoundInfo(request,player.getPid(),req.setID);
        if (!ErrorCode.Success.equals(result.getCode())) {
            request.response();
        }
    }
}	
	
