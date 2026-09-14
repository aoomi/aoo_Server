package core.network.client2game.handler.cdxzmj;				
				
import business.cdxzmj.c2s.iclass.CCDXZMJ_CreateRoom;	
import business.player.Player;	
import business.player.feature.PlayerRoom;	
import cenum.PrizeType;	
import com.ddm.server.websocket.def.ErrorCode;	
import com.ddm.server.websocket.handler.requset.WebSocketRequest;	
import com.google.gson.Gson;	
import core.network.client2game.handler.PlayerHandler;	
import core.network.http.proto.SData_Result;	
import core.server.cdxzmj.CDXZMJAPP;	
import jsproto.c2s.cclass.room.BaseRoomConfigure;	
	
import java.io.IOException;	
				
/**				
 * 创建房间				
 * 				
 * @author Administrator				
 *				
 */				
public class CCDXZMJCreateRoom extends PlayerHandler {				
	
	@SuppressWarnings("rawtypes")	
	@Override				
	public void handle(Player player, WebSocketRequest request, String message)				
			throws IOException {				
				
		final CCDXZMJ_CreateRoom clientPack = new Gson().fromJson(message,				
				CCDXZMJ_CreateRoom.class);				
		// 公共房间配置				
		BaseRoomConfigure<CCDXZMJ_CreateRoom> configure = new BaseRoomConfigure<CCDXZMJ_CreateRoom>(				
				PrizeType.RoomCard,				
				CDXZMJAPP.GameType(),				
				clientPack.clone());				
		SData_Result resule = player.getFeature(PlayerRoom.class).createRoomAndConsumeCard(configure);				
		if (ErrorCode.Success.equals(resule.getCode())) {				
			request.response(resule.getData());				
		} else {				
			request.error(resule.getCode(),resule.getMsg());				
		}				
	}				
}				
