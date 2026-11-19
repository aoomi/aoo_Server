package business.rocketmq.consumer;

import BaseCommon.CommLog;
import business.global.GM.MaintainServerMgr;
import business.rocketmq.bo.MqSetMaintainServerBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.rocketmq.MqConsumerHandler;

/**
 * @author : xushaojun
 * create at:  2020-10-30  16:30
 * @description: 紧急维护
 */
@Consumer(topic = MqTopic.SET_MAINTAIN_SERVER)
public class SetMaintainServerConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        CommLog.info("设置维护时间");
        MqSetMaintainServerBo bo = (MqSetMaintainServerBo) body;
        int startTime = bo.getStartTime();
        int endTime = bo.getEndTime();
        MaintainServerMgr.getInstance().setMaintainServer(startTime, endTime);
    }
}
