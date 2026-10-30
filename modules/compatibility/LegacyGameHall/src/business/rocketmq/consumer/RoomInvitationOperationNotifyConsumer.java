package business.rocketmq.consumer;

import business.global.shareroom.ShareRoom;
import business.global.shareroom.ShareRoomMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.rocketmq.bo.MqRoomInvitationOperationNotifyBo;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.common.utils.JsonUtil;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.room.RoomInvitationOperationEvent;

/**
 * @author : xushaojun
 * create at:  2020-09-04  10:00
 * @description: 邀请在线玩家通知
 */
@Deprecated
//@Consumer(topic = MqTopic.ROOM_INVITATION_OPERATION_NOTIFY)
public class RoomInvitationOperationNotifyConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqRoomInvitationOperationNotifyBo bo = (MqRoomInvitationOperationNotifyBo) body;
        CommLogD.info("邀请在线玩家通知[{}]", JsonUtil.toJson(bo));
        Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(bo.getPid());
        ShareRoom shareRoom = ShareRoomMgr.getInstance().getShareRoomByKey(bo.getRoomKey());
        if (player == null || shareRoom == null || shareRoom.getBaseRoomConfigure() == null
                || shareRoom.getBaseRoomConfigure().getGameType() == null) {
            CommLogD.error("邀请在线玩家通知上下文不存在, pid:{}, roomKey:{}", bo.getPid(), bo.getRoomKey());
            return;
        }
        // 通知玩家邀请
        DispatcherComponent.getInstance().publish(new RoomInvitationOperationEvent(bo.getPid(), player.getPid(), bo.getClubId(), bo.getUnionId(), shareRoom.getRoomKey(), shareRoom.getBaseRoomConfigure().getGameType().getId(), player.getName(), shareRoom.getBaseRoomConfigure().getBaseCreateRoom(), shareRoom.getRoomPidAll()));
    }
}
