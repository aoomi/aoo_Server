package core.network.client2game.handler.zypk;

import java.io.IOException;

import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;

import business.global.pk.zypk.ZYPKRoom;
import business.global.pk.zypk.ZYPKRoomSet;
import business.global.room.RoomMgr;
import business.player.Player;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.zypk.CZYPK_PlayerOp;

/*
 * 玩家操作
 * */

public class CZYPKPlayerOp extends PlayerHandler {

	@Override
	public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
		final CZYPK_PlayerOp clientPack = new Gson().fromJson(message, CZYPK_PlayerOp.class);
    	
		ZYPKRoom<?> room = (ZYPKRoom<?>) RoomMgr.getInstance().getRoom(clientPack.roomID);
    	if (null == room){
    		request.error(ErrorCode.NotAllow, "CZYPKPlayerOp not find room:"+clientPack.roomID);
    		return;
    	}
    	ZYPKRoomSet set =  (ZYPKRoomSet) room.getCurSet();
    	if(null == set){
    		request.error(ErrorCode.NotAllow, "CZYPKPlayerOp not set room:"+clientPack.roomID);
    		return;
    	}
    	set.onPlayerOp(request,  clientPack);		
	}
}
