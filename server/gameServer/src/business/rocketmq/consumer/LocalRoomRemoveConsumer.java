package business.rocketmq.consumer;

import business.rocketmq.bo.MqRoomRemoveBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.localcache.LocalRoomEvent;

/**
 * @author : xushaojun
 * create at:  2021-08-11  16:51
 * @description: 删除房间
 */
@Consumer(topic = MqTopic.LOCAL_ROOM_REMOVE)
public class LocalRoomRemoveConsumer implements MqConsumerHandler {

    @Override
    public void action(Object body) {
        MqRoomRemoveBo bo = (MqRoomRemoveBo) body;
//        CommLogD.info("删除房间{}", bo.getRoomKey());
        DispatcherComponent.getInstance().publish(new LocalRoomEvent(bo.getRoomKey(), null, false));
    }
}
