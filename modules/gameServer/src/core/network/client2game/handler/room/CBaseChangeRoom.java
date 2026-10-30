package core.network.client2game.handler.room;

import business.global.GM.MaintainGameMgr;
import business.global.config.GameListConfigMgr;
import business.global.sharegm.ShareNodeServerMgr;
import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.player.Player;
import business.player.feature.PlayerRoom;
import business.rocketmq.bo.MqAbsRequestBo;
import business.rocketmq.bo.MqChangeRoomBo;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.ShareNode;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerMgr;
import cenum.room.RoomState;
import com.ddm.server.common.Config;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.common.utils.EncryptUtils;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.exception.WSException;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.google.gson.Gson;
import core.config.server.GameTypeMgr;
import core.network.client2game.handler.PlayerHandler;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.iclass.room.CBase_EnterRoom;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

public class CBaseChangeRoom extends PlayerHandler {
    @Override
    public void handle(Player player, WebSocketRequest request, String message) throws WSException, IOException {
        final CBase_EnterRoom req = new Gson().fromJson(message, CBase_EnterRoom.class);
        if (Config.isShare()) {
            //获取进入房间对象
            ShareRoom enterShareRoom = ShareRoomMgr.getInstance().getShareRoomByKey(req.getRoomKey());
            if (null == enterShareRoom) {
                request.error(ErrorCode.NotFind_Room, "NotFind_Room");
                return;
            }
            String tempPassword = enterShareRoom.getPassword();
            if (StringUtils.isNotEmpty(tempPassword)) {
                if (StringUtils.isEmpty(req.getPassword()) || !tempPassword.equals(EncryptUtils.encryptDES(req.getPassword()))) {
                    request.error(ErrorCode.ROOM_PASSWORD_ERROR, "{\"tagId\": %d,\"clubId\": %d}", enterShareRoom.getTagId(), req.getClubId());
                    return;
                }
            }
            SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(player.getPid());
            //如果玩家已经退出房间
            if (sharePlayer.getRoomInfo().getRoomId() <= 0) {
                GameType enterGameType = GameTypeMgr.getInstance().gameType(enterShareRoom.getBaseRoomConfigure().getGameType().getId());
                if (GameListConfigMgr.getInstance().checkIsLiveByGameType(enterGameType.getId())) {
                    //检查游戏是否在维护中
                    SData_Result result = MaintainGameMgr.getInstance().checkMaintainGame(enterGameType.getId(), player);
                    if (!ErrorCode.Success.equals(result.getCode())) {
                        request.error(ErrorCode.Game_Maintain, MaintainGameMgr.getInstance().getMaintainGameContent(enterGameType.getId()));
                        return;
                    }
                    ShareNode shareNodeEnter = GameListConfigMgr.getInstance().getShareNodeByRoom(enterShareRoom);
                    MqAbsRequestBo mqAbsRequestBo = new MqAbsRequestBo(player.getPid(), enterGameType.getName(), enterGameType.getId(), message, request.getHeader().event, shareNodeEnter);
                    long requestId=Integer.toUnsignedLong(java.util.Objects.hash(player.getPid(),enterShareRoom.getRoomId(),request.getHeader().event,message));mqAbsRequestBo.setRequestId(requestId);
                    mqAbsRequestBo.setShareNodeFrom(ShareNodeServerMgr.getInstance().getThisNode());
                    //推送到MQ
                    var confirmation=MqProducerMgr.get().sendConfirmed(MqTopic.BASE_ENTER_ROOM + enterGameType.getId(),"enter-room:"+player.getPid()+":"+enterShareRoom.getRoomId()+":"+requestId,mqAbsRequestBo);
                    if(confirmation!=MqProducerMgr.Confirmation.CONFIRMED){request.error(confirmation==MqProducerMgr.Confirmation.UNKNOWN?ErrorCode.Request_OutcomeUnknown:ErrorCode.Request_DependencyUnavailable,"ROOM_REQUEST_NOT_CONFIRMED:"+confirmation);return;}
                } else {
                    request.error(ErrorCode.Server_Maintain, "SERVER_STARTING");
                }
            } else {
                if (sharePlayer.getRoomInfo().getRoomId() == enterShareRoom.getRoomId()) {
                    // 检查是否信息
                    request.error(ErrorCode.Exist_InRoom, "已经在房间里");
                    return;
                }
                if (!RoomState.Init.equals(enterShareRoom.getRoomState())) {
                    // 房间状态不对无法进入
                    request.error(ErrorCode.Room_STATUS_ERROR, "enterRoomOtherCondition RoomState:{%s}", enterShareRoom.getRoomState());
                    return;
                }
                //获取退出房间的游戏Id
                ShareRoom exitShareRoom = ShareRoomMgr.getInstance().getShareRoomByRoomId(sharePlayer.getRoomInfo().getRoomId());
                GameType exitGameType = GameTypeMgr.getInstance().gameType(exitShareRoom.getBaseRoomConfigure().getGameType().getId());
                GameType enterGameType = GameTypeMgr.getInstance().gameType(enterShareRoom.getBaseRoomConfigure().getGameType().getId());
                if (GameListConfigMgr.getInstance().checkIsLiveByGameType(enterGameType.getId())) {
                    //检查游戏是否在维护中
                    SData_Result result = MaintainGameMgr.getInstance().checkMaintainGame(enterGameType.getId(), player);
                    if (!ErrorCode.Success.equals(result.getCode())) {
                        request.error(ErrorCode.Game_Maintain, MaintainGameMgr.getInstance().getMaintainGameContent(enterGameType.getId()));
                        return;
                    }
                    ShareNode shareNodeExit = GameListConfigMgr.getInstance().getShareNodeByRoom(exitShareRoom);
                    ShareNode shareNodeEnter = GameListConfigMgr.getInstance().getShareNodeByRoom(enterShareRoom);
                    MqChangeRoomBo mqChangeRoomBo = new MqChangeRoomBo(player.getPid(), exitGameType.getId(), enterGameType.getId(), enterGameType.getName(), message, request.getHeader().event, shareNodeExit, shareNodeEnter);
                    mqChangeRoomBo.setShareNodeFrom(ShareNodeServerMgr.getInstance().getThisNode());
                    //推送到MQ
                    long requestId=Integer.toUnsignedLong(java.util.Objects.hash(player.getPid(),exitShareRoom.getRoomId(),enterShareRoom.getRoomId(),request.getHeader().event,message));
                    var confirmation=MqProducerMgr.get().sendConfirmed(MqTopic.BASE_CHANGE_ROOM + exitGameType.getId(),"change-room:"+player.getPid()+":"+exitShareRoom.getRoomId()+":"+enterShareRoom.getRoomId()+":"+requestId,mqChangeRoomBo);
                    if(confirmation!=MqProducerMgr.Confirmation.CONFIRMED){request.error(confirmation==MqProducerMgr.Confirmation.UNKNOWN?ErrorCode.Request_OutcomeUnknown:ErrorCode.Request_DependencyUnavailable,"ROOM_REQUEST_NOT_CONFIRMED:"+confirmation);return;}
                } else {
                    request.error(ErrorCode.Server_Maintain, "SERVER_STARTING");
                }
            }
            request.response();
        } else {
            SData_Result result = player.getFeature(PlayerRoom.class).onChange(req.getPosID(), req.getRoomKey(), req.getClubId(), req.getPassword());
            if (ErrorCode.Success.equals(result.getCode())) {
                request.response(result.getData());
            } else {
                request.error(result.getCode(), result.getMsg());
            }
        }
    }
}
