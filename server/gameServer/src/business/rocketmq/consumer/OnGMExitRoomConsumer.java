package business.rocketmq.consumer;

import business.global.sharegm.ShareNodeServerMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.rocketmq.bo.MqOnGMExitRoomMsg;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.rocketmq.MqConsumerHandler;

/**
 * @author : xushaojun
 * create at:  2020-11-27  10:00
 * @description: 后台踢出玩家
 */
@Consumer(topic = MqTopic.ON_GM_EXIT_ROOM)
public class OnGMExitRoomConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqOnGMExitRoomMsg bo = (MqOnGMExitRoomMsg) body;
        //检测当前节点
        if (ShareNodeServerMgr.getInstance().checkCurrentNode(bo.getShareNode().getIp(), bo.getShareNode().getPort())) {
            // 用户信息
            Player player = PlayerMgr.getInstance().getPlayer(bo.getPid());
            if (player == null) {
                return;
            }
            player.onGMExitRoom();
        }
    }

}
