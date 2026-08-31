package core.network.client2game.handler.room;

import BaseCommon.CommLog;
import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.player.Player;
import cenum.RoomTypeEnum;
import cenum.room.RoomState;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.room.CBase_RoomInvitationList;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

/**								
 * 房间记录								
 */
public abstract class CBaseRoomInvitationList extends PlayerHandler {
								
								
    @Override								
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {								
        final CBase_RoomInvitationList req = new Gson().fromJson(message, CBase_RoomInvitationList.class);								
        AbsBaseRoom room = RoomMgr.getInstance().getRoom(req.getRoomID());								
        // 检查房间是否存在													
        if (Objects.isNull(room)) {								
            request.error(ErrorCode.NotFind_Room, "CBaseRoomInvitationList NotFind_Room");
            return;								
        }								
        if (!RoomTypeEnum.checkUnionOrClub(room.getRoomTypeEnum())) {								
            request.error(ErrorCode.NotFind_Room, "CBaseRoomInvitationList is not club or union");
            return;								
        }								
        AbsRoomPos roomPos = room.getRoomPosMgr().getPosByPid(player.getPid());								
        if (Objects.isNull(roomPos)) {								
            request.error(ErrorCode.NotFind_Pos, "CBaseRoomInvitationList NotFind_Pos");
            return;								
        }								
        if (RoomState.Init.equals(room.getRoomState())) {
            request.response(RoomMgr.getInstance().getRoomInvitationItemList(req.getClubId(), req.getUnionId(), req.getPageNum(), "",req.getSize()).getData());								
        } else {
            request.response(Collections.emptyList());								
        }								
    }								
}													
