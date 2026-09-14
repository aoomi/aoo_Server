package core.network.client2game.handler.cdxzmj;				
				
import java.io.IOException;				
				
import com.ddm.server.websocket.def.ErrorCode;				
import com.ddm.server.websocket.handler.requset.WebSocketRequest;				
import com.google.gson.Gson;				
				
import business.global.mj.cdxzmj.CDXZMJRoom;				
import business.global.room.RoomMgr;				
import business.player.Player;				
import core.network.client2game.handler.PlayerHandler;				
import jsproto.c2s.iclass.room.CBase_RoomRecord;				
				
/**				
 * 房间记录				
 * @author Administrator				
 *				
 */				
public class CCDXZMJRoomRecord extends PlayerHandler {				
					
				
    @Override				
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {				
    	final CBase_RoomRecord req = new Gson().fromJson(message, CBase_RoomRecord.class);				
    					
    	CDXZMJRoom room = (CDXZMJRoom) RoomMgr.getInstance().getRoom(req.getRoomID());				
    	if (null == room){				
    		request.error(ErrorCode.NotAllow, "CCDXZMJRoomRecord not find room:"+req.getRoomID());				
    		return;				
    	}				
				
    	request.response(room.getRecord());				
    }				
}				
