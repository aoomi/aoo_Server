package business.rocketmq.consumer;

import business.global.sharegm.ShareNodeServerMgr;
import business.rocketmq.bo.MqUrgentMaintainServerBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.mq.factory.MqConsumerTopicFactory;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.other.UrgentMaintainServerEvent;

/**
 * @author : xushaojun
 * create at:  2020-10-30  16:30
 * @description: 紧急维护
 */
@Consumer(topic = MqTopic.URGENT_MAINTAIN_SERVER)
public class UrgentMaintainServerConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqUrgentMaintainServerBo mqUrgentMaintainServerBo = (MqUrgentMaintainServerBo) body;
        if (ShareNodeServerMgr.getInstance().checkCurrentNode(mqUrgentMaintainServerBo.getNodeIp(), mqUrgentMaintainServerBo.getNodePort())) {
            CommLogD.info("紧急维护节点{}:{}", mqUrgentMaintainServerBo.getNodeIp(), mqUrgentMaintainServerBo.getNodePort());
            MqConsumerTopicFactory.getInstance().stopCurConnect();
            //节点维护
            DispatcherComponent.getInstance().publish(new UrgentMaintainServerEvent());
        }

    }
}
