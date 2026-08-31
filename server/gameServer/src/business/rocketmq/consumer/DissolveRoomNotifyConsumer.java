package business.rocketmq.consumer;

import BaseThread.ThreadManager;
import business.global.room.NormalRoomMgr;
import business.global.room.base.RoomImpl;
import business.global.sharegm.ShareNodeServerMgr;
import business.rocketmq.bo.MqDissolveRoomNotifyBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import jsproto.c2s.cclass.club.Club_define;

/**
 * @author : xushaojun
 * create at:  2020-12-09  10:00
 * @description: 解散房间
 */
@Consumer(topic = MqTopic.DISSOLVE_ROOM_NOTIFY)
public class DissolveRoomNotifyConsumer implements MqConsumerHandler {
    @Override
    public void action(Object body) {
        MqDissolveRoomNotifyBo bo = (MqDissolveRoomNotifyBo) body;
        if (ShareNodeServerMgr.getInstance().checkCurrentNode(bo.getShareNode().getIp(), bo.getShareNode().getPort())) {
            ThreadManager.getInstance().regThread(Thread.currentThread().getId());
            RoomImpl roomImpl = NormalRoomMgr.getInstance().getNoneRoomByKey(bo.getRoomKey());
            roomImpl.doDissolveRoom(Club_define.Club_DISSOLVEROOM_STATUS.Club_DISSOLVEROOM_STATUS_CHANGE_ROOMCRG.value());
        }

    }

}
