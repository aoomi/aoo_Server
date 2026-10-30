package core.network.client2game.handler.cdxzmj;

import business.cdxzmj.c2s.iclass.CCDXZMJ_AutoChoose;
import business.global.mj.cdxzmj.CDXZMJRoom;
import business.global.room.RoomMgr;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;

public class CCDXZMJAutoChoose extends PlayerHandler {
    @Override	
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {	
        final CCDXZMJ_AutoChoose req = new Gson().fromJson(message, CCDXZMJ_AutoChoose.class);
        long roomID = req.roomID;	
	
	
        CDXZMJRoom room = (CDXZMJRoom) RoomMgr.getInstance().getRoom(roomID);
        if (null == room) {	
            request.error(ErrorCode.NotAllow, "CCXMJOpCard not find room:" + roomID);	
            return;	
        }	
        room.opAutoChoose(request, player.getId(), req);
    }	
}						
