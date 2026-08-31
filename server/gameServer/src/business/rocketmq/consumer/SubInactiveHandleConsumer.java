package business.rocketmq.consumer;

import BaseCommon.CommLog;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.Config;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.dispatcher.disruptor.SubWorkerPoolFactory;
import core.network.handle.SubInactiveHandle;

/**
 * 注意这个topic 和 Config.getLocalServer() 是一样的，所以如果修改要一起修改
 */
@Consumer(topic = Config.LOCAL_SERVER)
public class SubInactiveHandleConsumer implements MqConsumerHandler {
    @Override
    public void action(Object body) throws ClassNotFoundException {
        byte[] req = (byte[]) body;
        if (req.length <= 0) {
            CommLog.error("SubInactiveHandleConsumer body length <= 0");
            return;
        }
        boolean accepted = SubWorkerPoolFactory.getInstance().publish().tryPublish(new SubInactiveHandle(req));
        if (!accepted) {
            throw new IllegalStateException("Sub inactive dispatch queue is unavailable; retry message");
        }
    }
}
