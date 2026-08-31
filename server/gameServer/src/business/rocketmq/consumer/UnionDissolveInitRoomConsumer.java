package business.rocketmq.consumer;

import business.global.room.RoomMgr;
import business.rocketmq.bo.MqUnionDissolveInitRoomBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.rocketmq.MqConsumerHandler;

/**
 * @author : xushaojun
 * create at:  2021-09-15  10:00
 * @description: 亲友圈解散还没有开始房间
 */
@Consumer(topic = MqTopic.UNION_DISSOLVE_INIT_ROOM)
public class UnionDissolveInitRoomConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqUnionDissolveInitRoomBo bo = (MqUnionDissolveInitRoomBo) body;
        if (bo.getUnionId() > 0) {
            RoomMgr.getInstance().cleanAllRoomByUnionId(bo.getUnionId());
        }
    }
}
