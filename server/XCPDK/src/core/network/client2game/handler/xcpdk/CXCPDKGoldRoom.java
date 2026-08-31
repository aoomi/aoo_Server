package core.network.client2game.handler.xcpdk;

import business.player.Player;
import business.player.feature.PlayerGoldRoom;
import business.xcpdk.c2s.iclass.CXCPDK_CreateRoom;
import cenum.PrizeType;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.config.refdata.RefDataMgr;
import core.config.refdata.ref.RefPractice;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import core.server.xcpdk.XCPDKAPP;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.RobotRoomConfig;
import jsproto.c2s.iclass.room.CBase_GoldRoom;

import java.io.IOException;

/**
 * 创建房间
 * 
 * @author Administrator
 *
 */
public class CXCPDKGoldRoom extends PlayerHandler {

	@SuppressWarnings("rawtypes")
	@Override
	public void handle(Player player, WebSocketRequest request, String message) throws IOException {

		final CBase_GoldRoom clientPack = new Gson().fromJson(message, CBase_GoldRoom.class);
		RefPractice data = RefDataMgr.get(RefPractice.class, clientPack.getPracticeId());
		if (data == null) {
			request.error(ErrorCode.NotAllow, "CXCPDKGoldRoom do not find practiceId");
			return;
		}
		// 游戏配置
		CXCPDK_CreateRoom createClientPack = new CXCPDK_CreateRoom();
		// 练习场游戏人数
		createClientPack.setPlayerNum(3);
		// 公共房间配置
		BaseRoomConfigure<CXCPDK_CreateRoom> configure = new BaseRoomConfigure<CXCPDK_CreateRoom>(PrizeType.Gold,
				XCPDKAPP.GameType(), createClientPack.clone(), new RobotRoomConfig(data.getBaseMark(),data.getMin(),data.getMax(),clientPack.getPracticeId()));
		SData_Result resule = player.getFeature(PlayerGoldRoom.class).createAndQuery(configure);
		if (ErrorCode.Success.equals(resule.getCode())) {
			request.response(resule.getData());
		} else {
			request.error(resule.getCode(), resule.getMsg());
		}
	}
}
