package business.rocketmq.consumer;

import business.player.Player;
import business.player.PlayerMgr;
import business.rocketmq.bo.MqPlayerPushProtoBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.common.utils.JsonUtil;

/**
 * @author : xushaojun
 * create at:  2020-10-16  16:51
 * @description: 玩家推送通知
 */
@Consumer(topic = MqTopic.PLAYER_PUSH_PROTO)
public class PlayerPushProtoConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqPlayerPushProtoBo bo = (MqPlayerPushProtoBo) body;
        Player player = PlayerMgr.getInstance().getOnlinePlayerByPid(bo.getPid());
        if (player != null) {
            CommLogD.info("玩家推送通知[{}][{}]", JsonUtil.toJson(bo));
            player.pushProto(bo.getBaseSendMsgT());
        }
    }
}
