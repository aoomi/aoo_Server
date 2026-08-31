package business.rocketmq.consumer;

import business.global.config.GameListConfigMgr;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;

/**
 * @author : xushaojun
 * create at:  2020-10-12  10:00
 * @description: 重新加载游戏配置
 */
@Consumer(topic = MqTopic.HTTP_RELOAD_GAME_LIST_CONFIG)
public class ReloadGameListConfigConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        CommLogD.info("游戏配置更新");
        GameListConfigMgr.getInstance().init();
    }

}
