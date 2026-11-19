package core.network.client2game.handler.room;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.player.Player;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.room.CBase_DissolveRoomAgree;

import java.io.IOException;
import java.util.Objects;

/**			
 * 同意解散房间			
 */
public abstract class CBaseDissolveRoomAgree extends PlayerHandler {
				
			
    @SuppressWarnings("rawtypes")			
	@Override			
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {			
    	final CBase_DissolveRoomAgree req = new Gson().fromJson(message, CBase_DissolveRoomAgree.class);			
    	AbsBaseRoom room = RoomMgr.getInstance().getRoom(req.getRoomID());			
    	if (Objects.isNull(room)){
    		request.error(ErrorCode.ExitROOM_ERROR_NOTFINDROOM, "CBaseDissolveRoomAgree not find room:"+req.getRoomID());
    		return;			
    	}			
    	SData_Result result = room.dissolveRoomAgree(player.getPid());			
		if (ErrorCode.Success.equals(result.getCode())) {			
			request.response();			
		} else {			
			request.error(result.getCode(), result.getMsg());			
		}			
    }			
}			
