package business.rocketmq.consumer;

import business.global.sharegm.ShareNodeServerMgr;
import business.rocketmq.bo.MqKickOutGameBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.mq.factory.MqConsumerTopicFactory;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.other.KickOutGameEvent;

/**
 * @author : xushaojun
 * create at:  2020-11-13  16:30
 * @description: 踢出节点的游戏
 */
@Consumer(topic = MqTopic.KICK_OUT_GAME)
public class KickOutGameConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqKickOutGameBo bo = (MqKickOutGameBo) body;
        if (ShareNodeServerMgr.getInstance().checkCurrentNode(bo.getNodeIp(), bo.getNodePort())) {
            MqConsumerTopicFactory.getInstance().stopCurConnect();
            CommLogD.info("踢出节点{}:{} 游戏{}", bo.getNodeIp(), bo.getNodePort(), bo.getGameTypeId());
            DispatcherComponent.getInstance().publish(new KickOutGameEvent(bo.getGameTypeId()));
        }

    }
}
