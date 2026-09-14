package business.rocketmq.cdxzmj.consumer;							
							
import business.rocketmq.constant.MqTopic;							
import business.rocketmq.consumer.BaseEnterRoomConsumer;							
import com.ddm.server.annotation.Consumer;							
import com.ddm.server.common.rocketmq.MqConsumerHandler;							
import core.server.cdxzmj.CDXZMJAPP;							
							
/**							
 * @author : xushaojun							
 * create at:  2020-08-25  11:17							
 * @description: 模版麻将进入房间							
 */							
@Consumer(topic = MqTopic.BASE_ENTER_ROOM, id = CDXZMJAPP.gameTypeId)							
public class CDXZMJEnterRoomConsumer extends BaseEnterRoomConsumer implements MqConsumerHandler {							
							
    @Override							
    public void action(Object body) {							
        super.action(body, CDXZMJAPP.GameType().getId());							
    }							
}							
