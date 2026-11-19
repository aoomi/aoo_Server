package business.rocketmq.consumer;

import business.rocketmq.bo.MqRoomBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.localcache.LocalRoomEvent;

/**
 * @author : xushaojun
 * create at:  2021-08-11  16:51
 * @description: 修改房间
 */
@Consumer(topic = MqTopic.LOCAL_ROOM_ADD)
public class LocalRoomAddConsumer implements MqConsumerHandler {

    @Override
    public void action(Object body) {
        MqRoomBo bo = (MqRoomBo) body;
//        CommLogD.info("修改房间{}", bo.getShareRoom().getRoomKey());
        DispatcherComponent.getInstance().publish(new LocalRoomEvent(null, bo.getShareRoom(), true));
    }
}
