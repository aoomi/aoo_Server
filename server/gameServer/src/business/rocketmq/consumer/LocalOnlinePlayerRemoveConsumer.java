package business.rocketmq.consumer;

import business.rocketmq.bo.MqPlayerRemoveBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.localcache.LocalPlayerEvent;

/**
 * @author : xushaojun
 * create at:  2021-08-11  16:51
 * @description: 删除在线玩家
 */
@Consumer(topic = MqTopic.LOCAL_ONLINE_PLAYER_REMOVE)
public class LocalOnlinePlayerRemoveConsumer implements MqConsumerHandler {

    @Override
    public void action(Object body) {
        MqPlayerRemoveBo bo = (MqPlayerRemoveBo) body;
//        CommLogD.info("删除在线玩家{}", bo.getPid());
        DispatcherComponent.getInstance().publish(new LocalPlayerEvent(bo.getPid(), null, 3));
    }
}
