package core.network.client2game.handler.zypk;

import java.io.IOException;

import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;

import business.global.pk.zypk.ZYPKRoom;
import business.global.room.RoomMgr;
import business.player.Player;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.zypk.CZYPK_Zhuang;

/*
 * 庄家设置
 * */

public class CZYPKZhuang extends PlayerHandler {

	@Override
	public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
		final CZYPK_Zhuang clientPack = new Gson().fromJson(message, CZYPK_Zhuang.class);
    	
		ZYPKRoom<?> room = (ZYPKRoom<?>) RoomMgr.getInstance().getRoom(clientPack.roomID);
    	if (null == room){
    		request.error(ErrorCode.NotAllow, "CZYPKZhuang not find room:"+clientPack.roomID);
    		return;
    	}
    
    	room.onZhuang(request,  clientPack);		
	}
}
