package business.rocketmq.consumer;

import business.global.room.RoomMgr;
import business.rocketmq.bo.MqPLayerLostConnectNotifyBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.common.utils.JsonUtil;

/**
 * @author : xushaojun
 * create at:  2020-09-08  10:00
 * @description: 玩家是否连接
 */
@Consumer(topic = MqTopic.PLAYER_LOST_CONNECT_NOTIFY)
public class PlayerLostConnectNotifyConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqPLayerLostConnectNotifyBo bo = (MqPLayerLostConnectNotifyBo) body;
        CommLogD.info("玩家是否连接通知[{}]", JsonUtil.toJson(bo));
        // 更新掉线状态
        RoomMgr.getInstance().lostConnectNotifyShare(bo.getRoomId(), bo.getPid(), bo.getIsLostConnect());


    }
}
