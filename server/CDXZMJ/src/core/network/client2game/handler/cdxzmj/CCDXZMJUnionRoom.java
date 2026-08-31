package core.network.client2game.handler.cdxzmj;				
				
import business.player.Player;				
import business.player.feature.PlayerClubRoom;				
import business.player.feature.PlayerUnionRoom;				
import business.cdxzmj.c2s.iclass.CCDXZMJ_CreateRoom;				
import cenum.PrizeType;				
import com.ddm.server.websocket.handler.requset.WebSocketRequest;				
import com.google.gson.Gson;				
import core.network.client2game.handler.PlayerHandler;				
import core.server.cdxzmj.CDXZMJAPP;				
import jsproto.c2s.cclass.room.BaseRoomConfigure;				
				
import java.io.IOException;				
				
/**				
 * 亲友圈房间				
 * 				
 * @author Administrator				
 *				
 */				
public class CCDXZMJUnionRoom extends PlayerHandler {				
				
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
		player.getFeature(PlayerUnionRoom.class).createNoneUnionRoom(request,configure);				
	}				
}				
