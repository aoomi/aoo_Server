package core.network.client2game.handler.union;

import BaseCommon.CommLog;
import business.global.room.NormalRoomMgr;
import business.global.room.base.RoomImpl;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.global.union.UnionMgr;
import business.player.Player;
import business.rocketmq.bo.MqUnionDissolveRoomNotifyBo;
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
import jsproto.c2s.iclass.union.CUnion_DissolveRoom;

import java.io.IOException;

/**
 * 亲友圈解散房间
 *
 * @author Huaxing
 */
public class CUnionDissolveRoom extends PlayerHandler {


    @SuppressWarnings("rawtypes")
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws IOException {

        final CUnion_DissolveRoom req = new Gson().fromJson(message, CUnion_DissolveRoom.class);
        if (Config.isShare()) {
            int minister = UnionMgr.getInstance().getUnionMemberMgr().getMinister(player.getPid(), req.getClubId(), req.getUnionId());
            if (minister <= 0) {
                request.error(ErrorCode.UNION_NOT_EXIST_MEMBER, "UNION_NOT_EXIST_MEMBER");
                return;
            }
            ShareRoom room = ShareRoomMgr.getInstance().getShareRoomByKey(req.getRoomKey());
            if (null == room) {
                request.error(ErrorCode.ExitROOM_ERROR_NOTFINDROOM, "CUnionDissolveRoom not find roomKey :" + req.getRoomKey());
                return;
            }
            if (room.getSpecialRoomId() != req.getUnionId() || !RoomTypeEnum.UNION.equals(room.getRoomTypeEnum())) {
                request.error(ErrorCode.ExitROOM_ERROR_NOTFINDROOM, "CUnionDissolveRoom not find roomKey :" + req.getRoomKey());
                return;
            }
            //通知mq
            var event=new MqUnionDissolveRoomNotifyBo(req.getUnionId(), req.getClubId(), req.getRoomKey(), player.getPid(), player.getName());
            var confirmation=MqProducerMgr.get().sendConfirmed(MqTopic.UNION_DISSOLVE_ROOM_NOTIFY,"union-dissolve:"+player.getPid()+":"+req.getUnionId()+":"+req.getClubId()+":"+req.getRoomKey(),event);
            if(confirmation!=MqProducerMgr.Confirmation.CONFIRMED){request.error(confirmation==MqProducerMgr.Confirmation.UNKNOWN?ErrorCode.Request_OutcomeUnknown:ErrorCode.Request_DependencyUnavailable,"UNION_DISSOLVE_NOT_CONFIRMED:"+confirmation);return;}
            request.response();
        } else {
            int minister = UnionMgr.getInstance().getUnionMemberMgr().getMinister(player.getPid(), req.getClubId(), req.getUnionId());
            if (minister <= 0) {
                request.error(ErrorCode.UNION_NOT_EXIST_MEMBER, "UNION_NOT_EXIST_MEMBER");
                return;
            }
            RoomImpl room = NormalRoomMgr.getInstance().getNoneRoomByKey(req.getRoomKey());
            if (null == room) {
                request.error(ErrorCode.ExitROOM_ERROR_NOTFINDROOM, "CUnionDissolveRoom not find roomKey :" + req.getRoomKey());
                return;
            }
            if (room.getSpecialRoomId() != req.getUnionId() || !RoomTypeEnum.UNION.equals(room.getRoomTypeEnum())) {
                request.error(ErrorCode.ExitROOM_ERROR_NOTFINDROOM, "CUnionDissolveRoom not find roomKey :" + req.getRoomKey());
                return;
            }

//		房间@房间ID已被亲友圈管理@管理名称 解散
//		房间@房间ID已被群主@群主名称 解散
            String ministerName = minister == Club_define.Club_MINISTER.Club_MINISTER_CREATER.value() ? "创建者" : "管理员";
            String msg = String.format("房间@%s已被赛事%s@%s 解散", req.getRoomKey(), ministerName, player.getName());
            SData_Result result = room.specialDissolveRoom(req.getUnionId(), RoomTypeEnum.UNION, minister, msg);
            if (ErrorCode.Success.equals(result.getCode())) {
                CommLog.info("CUnionDissolveRoom Success Pid:{},UnionId:{},clubId:{},RoomKey:{},minister:{},msg:{}", player.getPid(), req.getUnionId(), req.getClubId(), req.getRoomKey(), minister, msg);
                request.response();
            } else {
                request.error(result.getCode(), result.getMsg());
            }
        }
    }
}
