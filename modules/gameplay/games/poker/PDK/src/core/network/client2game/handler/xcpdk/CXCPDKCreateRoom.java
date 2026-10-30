package core.network.client2game.handler.xcpdk;

import business.player.Player;
import business.player.feature.PlayerRoom;
import business.xcpdk.c2s.iclass.CXCPDK_CreateRoom;
import cenum.PrizeType;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import core.server.xcpdk.XCPDKAPP;
import jsproto.c2s.cclass.room.BaseRoomConfigure;

import java.io.IOException;

/**
 * 创建房间
 * 
 * @author Administrator
 *
 */
public class CXCPDKCreateRoom extends PlayerHandler {

	@SuppressWarnings("rawtypes")
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
		SData_Result resule = player.getFeature(PlayerRoom.class).createRoomAndConsumeCard(configure);
		if (ErrorCode.Success.equals(resule.getCode())) {
			request.response(resule.getData());
		} else {
			request.error(resule.getCode(),resule.getMsg());
		}
	}
}
