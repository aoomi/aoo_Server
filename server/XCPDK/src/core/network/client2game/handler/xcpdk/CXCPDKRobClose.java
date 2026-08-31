package core.network.client2game.handler.xcpdk;

import business.global.pk.xcpdk.XCPDKRoom;
import business.global.pk.xcpdk.XCPDKRoomSet;
import business.global.room.RoomMgr;
import business.player.Player;
import business.xcpdk.c2s.iclass.CXCPDK_RobClose;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;

import java.io.IOException;

/*
 * 抢关门
 * */

public class CXCPDKRobClose extends PlayerHandler{

	@Override
	public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
		final CXCPDK_RobClose clientPack = new Gson().fromJson(message, CXCPDK_RobClose.class);
    	
		XCPDKRoom room = (XCPDKRoom) RoomMgr.getInstance().getRoom(clientPack.roomID);
    	if (null == room){
    		request.error(ErrorCode.NotAllow, "CXCPDKRobClose not find room:"+clientPack.roomID);
    		return;
    	}
    	XCPDKRoomSet set =  (XCPDKRoomSet) room.getCurSet();
    	if(null == set){
    		request.error(ErrorCode.NotAllow, "CXCPDKRobClose not set room:"+clientPack.roomID);
    		return;
    	}
    	set.onRobClose(request,  clientPack);		
	}
}
