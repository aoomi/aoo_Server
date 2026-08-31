package business.rocketmq.njpdk.consumer;

import business.rocketmq.constant.MqTopic;
import business.rocketmq.consumer.BaseEnterRoomConsumer;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import core.server.njpdk.NJPDKAPP;

/**
 * @author : xushaojun
 * create at:  2020-08-25  11:17
 * @description: 红中麻将进入房间
 */
@Consumer(topic = MqTopic.BASE_ENTER_ROOM, id = NJPDKAPP.gameTypeId)
public class NJPDKEnterRoomConsumer extends BaseEnterRoomConsumer implements MqConsumerHandler {

    @Override
    public void action(Object body) {
        super.action(body, NJPDKAPP.GameType().getId());
    }
}
