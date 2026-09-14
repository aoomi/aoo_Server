package business.rocketmq.consumer;

import business.player.Player;
import business.player.PlayerMgr;
import business.rocketmq.bo.MqPlayerChangeNotifyBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.common.utils.JsonUtil;

import java.util.List;

/**
 * @author : xushaojun
 * create at:  2020-09-24  16:51
 * @description: 玩家信息变化通知
 */
@Consumer(topic = MqTopic.PLAYER_CHANGE_NOTIFY)
public class PlayerChangeConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqPlayerChangeNotifyBo bo = (MqPlayerChangeNotifyBo) body;
        Player player = PlayerMgr.getInstance().getPlayer(bo.getPid());
        if (player != null) {
            CommLogD.info("玩家信息变化通知[{}]", JsonUtil.toJson(bo));
            Object value = bo.getBaseSendMsg();
            if (!(value instanceof List<?> properties)) {
                CommLogD.error("玩家信息变化通知属性格式错误, pid:{}", bo.getPid());
                return;
            }
            List<jsproto.c2s.cclass.Player.Property> typedProperties = properties.stream()
                    .map(jsproto.c2s.cclass.Player.Property.class::cast)
                    .toList();
            player.pushPropertiesMq(typedProperties);
        }
    }
}
