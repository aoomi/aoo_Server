package business.rocketmq.consumer;

import business.global.config.DictionariesMapController;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;

/**
 * @author : xushaojun
 * create at:  2020-10-12  10:00
 * @description: 重新加载游戏配置
 */
@Consumer(topic = MqTopic.HTTP_RELOAD_DICTIONARIES_CONFIG)
public class ReloadDictionariesConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        CommLogD.info("通用配置更新");
        DictionariesMapController.reload();
    }

}
