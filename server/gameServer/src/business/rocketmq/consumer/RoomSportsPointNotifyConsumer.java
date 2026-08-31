package business.rocketmq.consumer;

import BaseThread.ThreadManager;
import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.player.Player;
import business.player.PlayerMgr;
import business.rocketmq.bo.MqRoomSportsPointNotifyBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.common.utils.JsonUtil;

import java.util.Objects;

/**
 * @author : xushaojun
 * create at:  2021-03-24  10:00
 * @description: 通知房间比赛分变化
 */
@Consumer(topic = MqTopic.ROOM_SPORTS_POINT_NOTIFY)
public class RoomSportsPointNotifyConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqRoomSportsPointNotifyBo mqPLayerCreateNotifyBo = (MqRoomSportsPointNotifyBo) body;
        Player player = PlayerMgr.getInstance().getPlayer(mqPLayerCreateNotifyBo.getPid());
        if (Objects.nonNull(player)) {
            ThreadManager.getInstance().regThread(Thread.currentThread().getId());
            CommLogD.info("房间比赛分变化[{}]", JsonUtil.toJson(mqPLayerCreateNotifyBo));
            // 在房间里并且增加房卡
            AbsBaseRoom room = RoomMgr.getInstance().getRoom(mqPLayerCreateNotifyBo.getRoomId());
            if (Objects.nonNull(room)) {
                if (room.getRoomPosMgr().notify2RoomSportsPointChange(mqPLayerCreateNotifyBo.getPid(), mqPLayerCreateNotifyBo.getMemberId(), mqPLayerCreateNotifyBo.getValue())) {
                    return;
                }
            }
        }

    }
}
