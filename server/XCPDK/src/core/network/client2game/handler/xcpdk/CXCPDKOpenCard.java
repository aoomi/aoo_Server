package core.network.client2game.handler.xcpdk;

import business.global.pk.xcpdk.XCPDKRoom;
import business.global.pk.xcpdk.XCPDKRoomSet;
import business.global.room.RoomMgr;
import business.player.Player;
import business.xcpdk.c2s.iclass.CXCPDK_OpenCard;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;

/**
 * 明牌
 * */

public class CXCPDKOpenCard extends PlayerHandler{

	@Override
	public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
		final CXCPDK_OpenCard clientPack = new Gson().fromJson(message, CXCPDK_OpenCard.class);
    	
		XCPDKRoom room = (XCPDKRoom) RoomMgr.getInstance().getRoom(clientPack.roomID);
    	if (null == room){
    		request.error(ErrorCode.NotAllow, "CXCPDKOpenCard not find room:"+clientPack.roomID);
    		return;
    	}
    	XCPDKRoomSet set =  (XCPDKRoomSet) room.getCurSet();
    	if(null == set){
    		request.error(ErrorCode.NotAllow, "CXCPDKOpenCard not set room:"+clientPack.roomID);
    		return;
    	}
		request.response();
//    	set.onOpenCard(request, clientPack);
	}
}
