package business.rocketmq.consumer;

import business.global.config.NetIpMgr;
import business.rocketmq.bo.MqUrgentMaintainServerBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import java.util.Objects;

@Consumer(topic = MqTopic.IP_SERVER_NODE_SET_NOTIFY)
public class IpServerNodeConsumer implements MqConsumerHandler {
    @Override
    public void action(Object body) throws ClassNotFoundException {
        MqUrgentMaintainServerBo ipServerBo = (MqUrgentMaintainServerBo) body;
        if (Objects.nonNull(ipServerBo)) {
            NetIpMgr.getInstance().add(ipServerBo.getNodeIp());
        }
    }
}
