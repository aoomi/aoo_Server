package core.network.client2game.handler.room;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.room.CBase_DissolveRoom;

import java.io.IOException;
import java.util.Objects;

/**			
 * 解散房间			
 */
public abstract class CBaseDissolveRoom extends PlayerHandler {
			
	@SuppressWarnings("rawtypes")			
	@Override			
	public void handle(Player player, WebSocketRequest request, String message) throws IOException {			
			
		final CBase_DissolveRoom req = new Gson().fromJson(message, CBase_DissolveRoom.class);			
			
		AbsBaseRoom room = RoomMgr.getInstance().getRoom(req.getRoomID());			
		if (Objects.isNull(room)) {
			request.error(ErrorCode.ExitROOM_ERROR_NOTFINDROOM, "CBaseDissolveRoom not find room:" + req.getRoomID());
			return;			
		}			
		SData_Result result = room.dissolveRoom(player.getPid());			
		if (ErrorCode.Success.equals(result.getCode())) {			
			request.response();			
		} else {			
			request.error(result.getCode(), result.getMsg());			
		}			
	}			
}			
