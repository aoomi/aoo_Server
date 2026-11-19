package business.rocketmq.cdxzmj.consumer;							
							
import business.rocketmq.constant.MqTopic;							
import business.rocketmq.consumer.BaseChangeRoomConsumer;							
import com.ddm.server.annotation.Consumer;							
import com.ddm.server.common.rocketmq.MqConsumerHandler;							
import core.server.cdxzmj.CDXZMJAPP;							
							
/**							
 * @author : xushaojun							
 * create at:  2020-09-04  10:00							
 * @description: 换房间的退出房间							
 */							
@Consumer(topic = MqTopic.BASE_CHANGE_ROOM, id = CDXZMJAPP.gameTypeId)							
public class CDXZMJChangeRoomConsumer extends BaseChangeRoomConsumer implements MqConsumerHandler {							
							
							
    @Override							
    public void action(Object body) {							
        super.action(body, CDXZMJAPP.GameType().getId());							
    }							
}							
