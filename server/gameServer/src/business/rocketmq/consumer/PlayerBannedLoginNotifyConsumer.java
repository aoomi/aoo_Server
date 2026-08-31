package business.rocketmq.consumer;

import business.player.Player;
import business.player.PlayerMgr;
import business.rocketmq.bo.MqPlayerRemoveBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.common.utils.JsonUtil;

import java.util.Objects;

/**
 * @author : xushaojun
 * create at:  2020-09-08  10:00
 * @description: 玩家是否连接
 */
@Consumer(topic = MqTopic.PLAYER_BANNED_LOGIN_NOTIFY)
public class PlayerBannedLoginNotifyConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqPlayerRemoveBo bo = (MqPlayerRemoveBo) body;
        CommLogD.info("踢下线并禁止登陆[{}]", JsonUtil.toJson(bo));
        // 更新掉线状态
        Player player = PlayerMgr.getInstance().getPlayer(bo.getPid());
        if (Objects.nonNull(player)) {
            player.disconnect();
        }
    }
}
