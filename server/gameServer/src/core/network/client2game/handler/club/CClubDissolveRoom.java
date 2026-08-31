package core.network.client2game.handler.club;

import BaseCommon.CommLog;
import business.global.club.ClubMgr;
import business.global.room.NormalRoomMgr;
import business.global.room.base.RoomImpl;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.player.Player;
import business.rocketmq.bo.MqClubDissolveRoomNotifyBo;
import business.rocketmq.constant.MqTopic;
import cenum.RoomTypeEnum;
import com.ddm.server.common.Config;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.Club_define;
import jsproto.c2s.iclass.club.CClub_DissolveRoom;

import java.io.IOException;

/**
 * 亲友圈解散房间
 *
 * @author Huaxing
 */
public class CClubDissolveRoom extends PlayerHandler {


    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {

        final CClub_DissolveRoom req = new Gson().fromJson(message, CClub_DissolveRoom.class);
        if (Config.isShare()) {
            String roomkey = req.roomKey;
            int minister = ClubMgr.getInstance().getClubMemberMgr().getMinister(req.clubId, player.getPid());
            if (minister <= 0) {
                request.error(ErrorCode.CLUB_NOTMINISTER, "you not minister");
                return;
            }
            ShareRoom room = ShareRoomMgr.getInstance().getShareRoomByKey(roomkey);
            if (null == room) {
                request.error(ErrorCode.ExitROOM_ERROR_NOTFINDROOM, "CBaseDissolveRoom not find roomKey :" + roomkey);
                return;
            }
            if (room.getSpecialRoomId() != req.clubId || !RoomTypeEnum.CLUB.equals(room.getRoomTypeEnum())) {
                request.error(ErrorCode.ExitROOM_ERROR_NOTFINDROOM, "CBaseDissolveRoom not find roomKey :" + roomkey);
                return;
            }
            //通知mq
            var event=new MqClubDissolveRoomNotifyBo(req.clubId, req.roomKey, player.getPid(), player.getName());
            var confirmation=MqProducerMgr.get().sendConfirmed(MqTopic.CLUB_DISSOLVE_ROOM_NOTIFY,"club-dissolve:"+player.getPid()+":"+req.clubId+":"+req.roomKey,event);
            if(confirmation!=MqProducerMgr.Confirmation.CONFIRMED){request.error(confirmation==MqProducerMgr.Confirmation.UNKNOWN?ErrorCode.Request_OutcomeUnknown:ErrorCode.Request_DependencyUnavailable,"CLUB_DISSOLVE_NOT_CONFIRMED:"+confirmation);return;}
            request.response();
        } else {
            String roomkey = req.roomKey;
            int minister = ClubMgr.getInstance().getClubMemberMgr().getMinister(req.clubId, player.getPid());
            if (minister <= 0) {
                request.error(ErrorCode.CLUB_NOTMINISTER, "you not minister");
                return;
            }
            RoomImpl room = NormalRoomMgr.getInstance().getNoneRoomByKey(roomkey);
            if (null == room) {
                request.error(ErrorCode.ExitROOM_ERROR_NOTFINDROOM, "CBaseDissolveRoom not find roomKey :" + roomkey);
                return;
            }
            if (room.getSpecialRoomId() != req.clubId || !RoomTypeEnum.CLUB.equals(room.getRoomTypeEnum())) {
                request.error(ErrorCode.ExitROOM_ERROR_NOTFINDROOM, "CBaseDissolveRoom not find roomKey :" + roomkey);
                return;
            }
//		房间@房间ID已被亲友圈管理@管理名称 解散
//		房间@房间ID已被群主@群主名称 解散
            String ministerName = minister == Club_define.Club_MINISTER.Club_MINISTER_CREATER.value() ? "圈主" : "管理员";
            String msg = String.format("房间@%s已被亲友圈%s@%s 解散", roomkey, ministerName, player.getName());
            SData_Result result = room.specialDissolveRoom(req.clubId, RoomTypeEnum.CLUB, minister, msg);
            if (ErrorCode.Success.equals(result.getCode())) {
                CommLog.info("CClubDissolveRoom Success Pid:{},ClubId:{},RoomKey:{},minister:{},msg:{}", player.getPid(), req.clubId, req.roomKey, minister, msg);
                request.response();
            } else {
                request.error(result.getCode(), result.getMsg());
            }
        }
    }
}
