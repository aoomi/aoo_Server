package business.rocketmq.consumer;

import BaseCommon.CommLog;
import business.rocketmq.bo.MqGameStartToHallNotifyBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.data.AbstractRefDataMgr;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.common.utils.JsonUtil;

/**
 * @author : xushaojun
 * create at:  2020-09-11  10:00
 * @description: 游戏节点启动接收通知
 */
@Consumer(topic = MqTopic.GAME_START_TO_HALL_NOTIFY)
public class GameStartToHallConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqGameStartToHallNotifyBo bo = (MqGameStartToHallNotifyBo) body;
        CommLog.info("游戏节点启动接收通知[{}]", JsonUtil.toJson(bo));
        AbstractRefDataMgr.getInstance().reload();

    }
}
