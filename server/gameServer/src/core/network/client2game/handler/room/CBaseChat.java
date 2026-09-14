package core.network.client2game.handler.room;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.player.Player;
import cenum.ChatType;
import cenum.room.GaoJiTypeEnum;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.C1104_Chat;

import java.io.IOException;
import java.util.Objects;

public abstract class CBaseChat extends PlayerHandler {
			
	@SuppressWarnings("rawtypes")			
	@Override			
	public void handle(Player player, WebSocketRequest request, String message) throws IOException, WSException {			
		final C1104_Chat req = new Gson().fromJson(message, C1104_Chat.class);			
		ChatType chatType = ChatType.valueOf(req.type);			
		AbsBaseRoom room = RoomMgr.getInstance().getRoom(req.targetID);
		if (Objects.isNull(room)) {
			request.error(ErrorCode.NotAllow, "CBaseChat room you in room:" + req.targetID);
			return;			
    	}
		if (room.checkGaoJiXuanXiang(GaoJiTypeEnum.FORBIDDEN_WORDS)) {
			request.error(ErrorCode.NotAllow, "CBaseChat room you in CBaseChat:" + req.targetID);
			return;
		}
		SData_Result result = room.opChat(player, req.content, chatType, req.targetID, req.quickID);			
		if (ErrorCode.Success.equals(result.getCode())) {			
			request.response();			
		} else {			
			request.error(result.getCode(), result.getMsg());			
		}			
	}			
}			
