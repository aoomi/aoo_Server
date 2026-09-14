package business.rocketmq.consumer;

import business.global.sharegm.ShareNodeServerMgr;
import business.rocketmq.bo.MqUrgentMaintainServerBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.mq.factory.MqConsumerTopicFactory;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.other.KickOutServerEvent;

/**
 * @author : xushaojun
 * create at:  2020-11-13  16:30
 * @description: 踢出节点
 */
@Consumer(topic = MqTopic.KICK_OUT_SERVER)
public class KickOutServerConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqUrgentMaintainServerBo mqUrgentMaintainServerBo = (MqUrgentMaintainServerBo) body;
        if (ShareNodeServerMgr.getInstance().checkCurrentNode(mqUrgentMaintainServerBo.getNodeIp(), mqUrgentMaintainServerBo.getNodePort())) {
            CommLogD.info("踢出节点{}:{}", mqUrgentMaintainServerBo.getNodeIp(), mqUrgentMaintainServerBo.getNodePort());
            MqConsumerTopicFactory.getInstance().stopCurConnect();
            DispatcherComponent.getInstance().publish(new KickOutServerEvent());
        }

    }
}
