package core.network.client2game.handler.room;

import business.player.Player;
import business.player.feature.PlayerRoom;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.room.CBase_EnterRoom;

import java.io.IOException;

/**			
 * 进入房间			
 */
public abstract class CBaseGameEnterRoom extends PlayerHandler {
			
	@SuppressWarnings("rawtypes")			
	@Override			
	public void handle(Player player, WebSocketRequest request, String message) throws IOException {			
		final CBase_EnterRoom req = new Gson().fromJson(message, CBase_EnterRoom.class);			
		// 进入房间			
		SData_Result result = player.getFeature(PlayerRoom.class).findAndEnter(req.getPosID(), req.getRoomKey(),req.getClubId(),req.getPassword());			
		if (ErrorCode.Success.equals(result.getCode())) {			
			request.response(result.getData());			
		} else {			
			request.error(result.getCode(), result.getMsg());			
		}			
	}			
			
}			
