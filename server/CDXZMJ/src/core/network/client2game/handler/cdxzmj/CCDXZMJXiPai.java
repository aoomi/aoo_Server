package core.network.client2game.handler.cdxzmj;				
				
import java.io.IOException;				
				
import com.ddm.server.common.GameConfig;				
import com.ddm.server.websocket.def.ErrorCode;				
import com.ddm.server.websocket.handler.requset.WebSocketRequest;				
import com.google.gson.Gson;				
				
import business.global.room.RoomMgr;				
import business.global.room.base.AbsBaseRoom;				
import business.player.Player;				
import business.player.feature.PlayerCurrency;				
import cenum.ItemFlow;				
import cenum.PrizeType;				
import core.network.client2game.handler.PlayerHandler;				
import core.network.http.proto.SData_Result;				
import jsproto.c2s.iclass.room.CBase_GetRoomInfo;				
				
public class CCDXZMJXiPai extends PlayerHandler {				
					
				
    @SuppressWarnings("rawtypes")				
	@Override				
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {				
    	final CBase_GetRoomInfo req = new Gson().fromJson(message, CBase_GetRoomInfo.class);				
    	long roomID = req.getRoomID();				
    	AbsBaseRoom room = RoomMgr.getInstance().getRoom(roomID);				
    	if (null == room) {				
			request.error(ErrorCode.NotAllow, "CCDXZMJXiPai not find room:" + roomID);				
			return;				
		}				
		SData_Result result = room.opXiPai(player.getPid());				
		if (ErrorCode.Success.equals(result.getCode())) {				
			request.response();				
		} else {				
			request.error(result.getCode(), result.getMsg());				
		}				
    }				
}				
				
