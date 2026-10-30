package core.network.client2game.handler.room;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.room.CBase_Voice;

import java.io.IOException;
import java.util.Objects;

/**			
 * 玩家语音
 */			
public abstract class CBaseVoice extends PlayerHandler {
			
	@SuppressWarnings("rawtypes")			
	@Override			
	public void handle(Player player, WebSocketRequest request, String message)			
			throws WSException, IOException {			
    	final CBase_Voice req = new Gson().fromJson(message, CBase_Voice.class);			
    	long roomID = req.roomID;			
    	String url = req.url;			
    				
			
		AbsBaseRoom room = RoomMgr.getInstance().getRoom(roomID);
		if (Objects.isNull(room)) {
			request.error(ErrorCode.NotAllow, "CBaseVoice not find room:" + roomID);
			return;			
		}			
		SData_Result result = room.opRoomVoice(player.getPid(), url);			
		if (ErrorCode.Success.equals(result.getCode())) {			
			request.response();			
		} else {			
			request.error(result.getCode(), result.getMsg());			
		}			
	}			
			
}			
