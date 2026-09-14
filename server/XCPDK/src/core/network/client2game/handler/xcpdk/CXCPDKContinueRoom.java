package core.network.client2game.handler.xcpdk;

import business.global.room.ContinueRoomInfoMgr;
import business.xcpdk.c2s.iclass.CXCPDK_ContinueRoom;
import business.xcpdk.c2s.iclass.CXCPDK_CreateRoom;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerRoom;
import cenum.PrizeType;
import cenum.room.RoomContinueEnum;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import core.server.xcpdk.XCPDKAPP;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.ContinueRoomInfo;
import jsproto.c2s.iclass.room.CBase_ContinueRoom;
import jsproto.c2s.iclass.room.SRoom_ContinueRoom;
import jsproto.c2s.iclass.room.SRoom_ContinueRoomInfo;
import jsproto.c2s.iclass.room.SRoom_CreateRoom;

import java.io.IOException;

/**
 * 继续房间功能
 * 
 * @author Administrator
 *
 */
public class CXCPDKContinueRoom extends PlayerHandler {

	@SuppressWarnings("rawtypes")
	@Override
	public void handle(Player player, WebSocketRequest request, String message)
			throws IOException {

		final CBase_ContinueRoom continueRoom = new Gson().fromJson(message,
				CBase_ContinueRoom.class);
		ContinueRoomInfo continueRoomInfo= player.getFeature(PlayerRoom.class).getContinueRoomInfo(continueRoom.roomID);
		//如果找不到的话 说明已经被删除了  过了十分钟的有效时间
		if(continueRoomInfo==null){
			request.error(ErrorCode.Object_IsNull, "ContinueRoomInfo Not Find",continueRoom.roomID);
			return;
		}
		//找到的话已经被使用了
		if(continueRoomInfo.isUseFlag()){
			request.error(ErrorCode.NotAllow, "ContinueRoomInfo has been used",continueRoom.roomID);
			return;
		}
		CXCPDK_CreateRoom createRoom=(CXCPDK_CreateRoom)continueRoomInfo.getBaseRoomConfigure().getBaseCreateRoom();
		createRoom.setPaymentRoomCardType(continueRoom.continueType);
//		 公共房间配置
		BaseRoomConfigure<CXCPDK_CreateRoom> configure = new BaseRoomConfigure<>(
				PrizeType.RoomCard,
				XCPDKAPP.GameType(),
				createRoom.clone());
		SData_Result result = player.getFeature(PlayerRoom.class).continueRoom(configure, continueRoomInfo, continueRoom);
		if (ErrorCode.Success.equals(result.getCode())) {
			//创建成功的时候
			request.response(result.getData());
		} else {
			request.error(result.getCode(),result.getMsg());
		}
	}
}
