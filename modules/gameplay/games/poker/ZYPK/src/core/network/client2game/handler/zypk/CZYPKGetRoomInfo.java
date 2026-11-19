package core.network.client2game.handler.zypk;

import java.io.IOException;

import jsproto.c2s.iclass.CWZMJ_GetRoomInfo;
import business.global.room.RoomMgr;
import business.global.room.delegate.RoomDelegateAbstract;
import business.global.room.delegate.RoomPosDelegateAbstract;
import business.player.Player;
import business.player.feature.PlayerRoom;
import BaseCommon.CommLog;

import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;

import core.network.client2game.handler.PlayerHandler;

/**
 * 获取房间信息
 * @author Huaxing
 *
 */
@Deprecated
public class CZYPKGetRoomInfo extends PlayerHandler {


    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
		business.global.protocol.LegacyProtocolUsage.record("zypk.get_room_info");
    	
    	final CWZMJ_GetRoomInfo req = new Gson().fromJson(message, CWZMJ_GetRoomInfo.class);
    	long roomID = req.roomID;
		long boundRoomId = player.getFeature(PlayerRoom.class).getRoomID();
		if (boundRoomId <= 0L || roomID != boundRoomId) {
			CommLog.error("Rejected ZYPK cross-room snapshot request, requested:{}, bound:{}, pid:{}",
					roomID, boundRoomId, player.getPid());
			request.error(ErrorCode.NotAllow, "room session does not match requested room");
			return;
		}
    	
    	RoomDelegateAbstract<?> room = RoomMgr.getInstance().getRoom(roomID);
    	if (null == room){
    		request.error(ErrorCode.NotFind_Room, "CZYPKGetRoomInfo not find room:" + roomID);
    		return;
    	}
    	
        RoomPosDelegateAbstract<?> roomPos=room.getPosMgr().getPosByPid(player.getPid());
        if (null == roomPos) {
        	request.error(ErrorCode.NotFind_Player, "CZYPKGetRoomInfo not find room:" + roomID);
        	return;
        }
        request.response(room.getAllDisplayInfo(player.getPid()));// 无账号情况下返回为空
    }
}
