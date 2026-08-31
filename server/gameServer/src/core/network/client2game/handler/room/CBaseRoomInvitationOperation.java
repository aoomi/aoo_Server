package core.network.client2game.handler.room;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.player.Player;
import cenum.RoomTypeEnum;
import cenum.room.RoomState;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.room.RoomInvitationOperationEvent;
import core.network.client2game.handler.PlayerHandler;
import jsproto.c2s.iclass.room.CBase_RoomInvitationOperation;

import java.io.IOException;
import java.util.Objects;

/**								
 * 房间邀请操作
 */								
public abstract class CBaseRoomInvitationOperation extends PlayerHandler {
								
    @Override								
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {								
        final CBase_RoomInvitationOperation req = new Gson().fromJson(message, CBase_RoomInvitationOperation.class);								
        AbsBaseRoom room = RoomMgr.getInstance().getRoom(req.getRoomID());								
        // 检查房间是否存在													
        if (Objects.isNull(room)) {								
            request.error(ErrorCode.NotFind_Room, "CBaseRoomInvitationOperation NotFind_Room");
            return;								
        }								
        if (!RoomTypeEnum.checkUnionOrClub(room.getRoomTypeEnum())) {								
            request.error(ErrorCode.NotFind_Room, "CBaseRoomInvitationOperation is not club or union");
            return;								
        }								
        AbsRoomPos roomPos = room.getRoomPosMgr().getPosByPid(player.getPid());								
        if (Objects.isNull(roomPos)) {								
            request.error(ErrorCode.NotFind_Pos, "CBaseRoomInvitationOperation NotFind_Pos");
            return;								
        }								
        request.response();								
        if (RoomState.Init.equals(room.getRoomState())) {								
            // 通知玩家邀请													
            DispatcherComponent.getInstance().publish(new RoomInvitationOperationEvent(req.getPid(), player.getPid(), req.getClubId(), req.getUnionId(), room.getRoomKey(), room.getBaseRoomConfigure().getGameType().getId(), player.getName(), room.getBaseRoomConfigure().getBaseCreateRoomT(), room.getRoomPidAll()));								
        }								
    }								
}													
