package business.rocketmq.consumer;

import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.data.AbstractRefDataMgr;
import com.ddm.server.common.rocketmq.MqConsumerHandler;

/**
 * @author : xushaojun
 * create at:  2020-10-12  10:00
 * @description: 重新加载配置表
 */
@Consumer(topic = MqTopic.HTTP_RELOAD_CONFIG)
public class ReloadConfigConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        CommLogD.info("配置表加载");
        AbstractRefDataMgr.getInstance().reload();
    }

}
