package core.network.client2game.handler.room;

import BaseCommon.CommLog;
import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.room.CBase_Trusteeship;

import java.io.IOException;
import java.util.Objects;

/**			
 * 			
 * 玩家是否托管
 */			
public abstract class CBaseTrusteeship extends PlayerHandler {
			
	@SuppressWarnings("rawtypes")			
	@Override			
	public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {			
		final CBase_Trusteeship req = new Gson().fromJson(message, CBase_Trusteeship.class);			
		long roomID = req.roomID;			
		boolean trusteeship = req.trusteeship;			
			
		AbsBaseRoom room = RoomMgr.getInstance().getRoom(roomID);
		if (Objects.isNull(room)) {
			CommLog.error("CBaseTrusteeship error roomId:{}",roomID);
			request.error(ErrorCode.NotAllow, "CBaseTrusteeship not find room:" + roomID);
			return;			
		}			
		SData_Result result = room.opRoomTrusteeship(player.getPid(), trusteeship);			
		if (ErrorCode.Success.equals(result.getCode())) {			
			request.response();			
		} else {
			CommLog.error("CBaseTrusteeship error roomId:{},code:{},msg:{}",roomID,result.getCode().value(),result.getMsg());
			request.error(result.getCode(), result.getMsg());			
		}			
	}			
			
}			
