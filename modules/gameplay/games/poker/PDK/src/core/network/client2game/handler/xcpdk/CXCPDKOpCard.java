package core.network.client2game.handler.xcpdk;

import java.io.IOException;

import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;

import business.xcpdk.c2s.iclass.CXCPDK_OpCard;
import business.global.pk.xcpdk.XCPDKRoom;
import business.global.pk.xcpdk.XCPDKRoomSet;
import business.global.pk.xcpdk.XCPDKRoomSetSound;
import business.global.room.RoomMgr;
import business.player.Player;
import core.network.client2game.handler.PlayerHandler;

/**
 * 操作牌
 * */

public class CXCPDKOpCard extends PlayerHandler{

	@Override
	public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
		final CXCPDK_OpCard clientPack = new Gson().fromJson(message, CXCPDK_OpCard.class);
    	
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
    	XCPDKRoomSetSound sound = set.getCurRound();
    	if(null == sound){
    		request.error(ErrorCode.NotAllow, "CXCPDKOpenCard not sound room:"+clientPack.roomID);
    		return;
    	}
    	sound.onOpCard(request,  clientPack,true);
	}
}
