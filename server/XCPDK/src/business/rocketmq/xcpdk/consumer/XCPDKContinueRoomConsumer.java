package business.rocketmq.xcpdk.consumer;

import business.rocketmq.constant.MqTopic;
import business.rocketmq.consumer.BaseContinueRoomConsumer;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import core.server.xcpdk.XCPDKAPP;

/**
 * @author : xushaojun
 * create at:  2020-11-4  11:17
 * @description: 继续房间
 */
@Consumer(topic = MqTopic.BASE_CONTINUE_ROOM, id = XCPDKAPP.gameTypeId)
public class XCPDKContinueRoomConsumer extends BaseContinueRoomConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) throws ClassNotFoundException {
        super.action(body, XCPDKAPP.GameType());

    }
}
