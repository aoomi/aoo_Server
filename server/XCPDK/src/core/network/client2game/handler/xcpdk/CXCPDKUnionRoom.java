package core.network.client2game.handler.xcpdk;

import business.xcpdk.c2s.cclass.XCPDK_define;
import business.xcpdk.c2s.iclass.CXCPDK_CreateRoom;
import business.player.Player;
import business.player.feature.PlayerUnionRoom;
import cenum.PrizeType;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.server.xcpdk.XCPDKAPP;
import jsproto.c2s.cclass.room.BaseRoomConfigure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 亲友圈房间
 * 
 * @author Administrator
 *
 */
public class CXCPDKUnionRoom extends PlayerHandler {

	@Override
	public void handle(Player player, WebSocketRequest request, String message)
			throws IOException {

		final CXCPDK_CreateRoom clientPack = new Gson().fromJson(message,
				CXCPDK_CreateRoom.class);
		// 公共房间配置
		BaseRoomConfigure<CXCPDK_CreateRoom> configure = new BaseRoomConfigure<CXCPDK_CreateRoom>(
				PrizeType.RoomCard,
				XCPDKAPP.GameType(),
				clientPack.clone());
		player.getFeature(PlayerUnionRoom.class).createNoneUnionRoom(request,configure);
	}
}
