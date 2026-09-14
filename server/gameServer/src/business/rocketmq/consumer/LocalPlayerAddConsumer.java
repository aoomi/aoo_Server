package business.rocketmq.consumer;

import business.rocketmq.bo.MqPlayerBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.localcache.LocalPlayerEvent;

/**
 * @author : xushaojun
 * create at:  2021-08-11  16:51
 * @description: 修改玩家
 */
@Consumer(topic = MqTopic.LOCAL_PLAYER_ADD)
public class LocalPlayerAddConsumer implements MqConsumerHandler {

    @Override
    public void action(Object body) {
        MqPlayerBo bo = (MqPlayerBo) body;
//        CommLogD.info("修改玩家{}", bo.getSharePlayer().getPlayerBO().getId());
        DispatcherComponent.getInstance().publish(new LocalPlayerEvent(null, bo.getSharePlayer(), 1));
    }
}
