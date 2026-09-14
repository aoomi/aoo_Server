package business.rocketmq.consumer;

import business.global.sharegm.ShareNodeServerMgr;
import business.rocketmq.bo.MqUrgentMaintainServerBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.mq.factory.MqConsumerTopicFactory;
import core.dispatch.DispatcherComponent;
import core.dispatch.event.other.StopServerEvent;

/**
 * @author : xushaojun
 * create at:  2020-11-13  16:30
 * @description: 关闭节点
 */
@Consumer(topic = MqTopic.STOP_SERVER)
public class StopServerConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqUrgentMaintainServerBo mqUrgentMaintainServerBo = (MqUrgentMaintainServerBo) body;
        if (ShareNodeServerMgr.getInstance().checkCurrentNode(mqUrgentMaintainServerBo.getNodeIp(), mqUrgentMaintainServerBo.getNodePort()) && ShareNodeServerMgr.getInstance().checkIsLiveCurrent()) {
            CommLogD.info("关闭节点{}:{}", mqUrgentMaintainServerBo.getNodeIp(), mqUrgentMaintainServerBo.getNodePort());
            MqConsumerTopicFactory.getInstance().stopCurConnect();
            //关闭节点
            DispatcherComponent.getInstance().publish(new StopServerEvent());
        }

    }
}
