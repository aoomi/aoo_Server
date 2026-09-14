package core.network.client2game.handler.scjymj;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.player.Player;
import business.player.feature.PlayerRoom;
import com.ddm.server.common.CommLogD;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.room.CBase_GetRoomInfo;

import java.io.IOException;

/**
 * 获取济宁房间信息
 *
 * @author Huaxing
 */
@Deprecated
public class CSCJYMJGetRoomInfo extends PlayerHandler {


    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {
        business.global.protocol.LegacyProtocolUsage.record("scjymj.get_room_info");

        final CBase_GetRoomInfo req = new Gson().fromJson(message, CBase_GetRoomInfo.class);
        long roomID = req.getRoomID();
        long boundRoomId = player.getFeature(PlayerRoom.class).getRoomID();
        if (boundRoomId <= 0L || roomID != boundRoomId) {
            CommLogD.error("Rejected SCJYMJ cross-room snapshot request, requested:{}, bound:{}, pid:{}",
                    roomID, boundRoomId, player.getPid());
            request.error(ErrorCode.NotAllow, "room session does not match requested room");
            return;
        }
        AbsBaseRoom room = null;
        room = RoomMgr.getInstance().getRoom(roomID);
        PlayerRoom playerRoom = null;
        if (null == room) {
            // 获取玩家房间信息
            playerRoom = player.getFeature(PlayerRoom.class);
            if (null != playerRoom) {
                // 检查进入的房间和玩家身上的房间一致就解散房间
                if (roomID == playerRoom.getRoomID()) {
                    player.onGMExitRoom();
                }
            }
            CommLogD.error("CSCJYMJGetRoomInfo not find room:" + roomID);
            request.error(ErrorCode.NotFind_Room, "CSCJYMJGetRoomInfo not find room:" + roomID);
            return;
        }
        AbsRoomPos roomPos = room.getRoomPosMgr().getPosByPid(player.getPid());
        if (null == roomPos) {
            // 获取玩家房间信息
            playerRoom = player.getFeature(PlayerRoom.class);
            if (null != playerRoom) {
                // 检查进入的房间和玩家身上的房间一致就解散房间
                if (roomID == playerRoom.getRoomID()) {
                    player.onGMExitRoom();
                }
            }
            CommLogD.error("ChbMJGetRoomInfo not find NotFind_Player room:" + roomID);
            request.error(ErrorCode.NotFind_Player, "CJSYZMJGetRoomInfo not find NotFind_Player room:" + roomID);
            return;
        }
        request.response(room.getRoomInfo(player.getPid()));// 无账号情况下返回为空
    }
}
