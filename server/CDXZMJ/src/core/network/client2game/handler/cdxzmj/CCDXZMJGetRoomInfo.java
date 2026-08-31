package core.network.client2game.handler.cdxzmj;				
				
import java.io.IOException;				
				
import com.ddm.server.websocket.def.ErrorCode;				
import com.ddm.server.websocket.handler.requset.WebSocketRequest;				
import com.google.gson.Gson;				
				
import business.global.room.RoomMgr;				
import business.global.room.base.AbsBaseRoom;				
import business.global.room.base.AbsRoomPos;				
import business.player.Player;
import business.player.feature.PlayerRoom;
import BaseCommon.CommLog;
import core.network.client2game.handler.PlayerHandler;				
import jsproto.c2s.iclass.room.CBase_GetRoomInfo;				
				
/**				
 * 获取房间信息				
 * 				
 * @author Administrator				
 *				
 */				
@Deprecated
public class CCDXZMJGetRoomInfo extends PlayerHandler {
				
	@Override				
	public void handle(Player player, WebSocketRequest request, String message) throws IOException {
		business.global.protocol.LegacyProtocolUsage.record("cdxzmj.get_room_info");
				
		final CBase_GetRoomInfo req = new Gson().fromJson(message, CBase_GetRoomInfo.class);
		long boundRoomId = player.getFeature(PlayerRoom.class).getRoomID();
		if (boundRoomId <= 0L || req.getRoomID() != boundRoomId) {
			CommLog.error("Rejected CDXZMJ cross-room snapshot request, requested:{}, bound:{}, pid:{}",
					req.getRoomID(), boundRoomId, player.getPid());
			request.error(ErrorCode.NotAllow, "room session does not match requested room");
			return;
		}
		AbsBaseRoom room = RoomMgr.getInstance().getRoom(req.getRoomID());
		// 检查房间是否存在				
		if (null == room) {				
			request.error(ErrorCode.NotFind_Room, "CCDXZMJGetRoomInfo null == room");				
			return;				
		}				
		AbsRoomPos roomPos = room.getRoomPosMgr().getPosByPid(player.getPid());
		if (null == roomPos) {
			roomPos = room.getRoomPosMgr().getWatchPosByPid(player.getPid());
		}
		// 检查玩家是否在房间中				
		if (null == roomPos) {				
			request.error(ErrorCode.NotFind_Player, "CCDXZMJGetRoomInfo null == roomPos");				
			return;				
		}				
		// 获取房间信息				
		request.response(room.getRoomInfo(player.getPid()));// 无账号情况下返回为空				
	}				
}				
