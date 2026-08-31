package business.rocketmq.consumer;

import business.global.sharegm.ShareNewNodeServerMgr;
import business.rocketmq.bo.MqUrgentMaintainServerBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.rocketmq.MqConsumerHandler;

@Consumer(topic = MqTopic.STOP_SHARE_NODE)
public class StopShareNodeConsumer implements MqConsumerHandler {
    @Override
    public void action(Object body) throws ClassNotFoundException {
        MqUrgentMaintainServerBo mqUrgentMaintainServerBo = (MqUrgentMaintainServerBo) body;
        if(ShareNewNodeServerMgr.getInstance().checkCurrentNode(mqUrgentMaintainServerBo.getNodeIp(),mqUrgentMaintainServerBo.getNodePort())) {
            ShareNewNodeServerMgr.getInstance().stopCurNodeServer();
        }
    }
}
