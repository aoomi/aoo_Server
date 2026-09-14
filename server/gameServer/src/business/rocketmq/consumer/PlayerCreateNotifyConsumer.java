package business.rocketmq.consumer;

import BaseThread.ThreadManager;
import business.player.Player;
import business.player.PlayerMgr;
import business.rocketmq.bo.MqPLayerCreateNotifyBo;
import business.rocketmq.constant.MqTopic;
import business.shareplayer.SharePlayer;
import business.shareplayer.SharePlayerBO;
import business.shareplayer.SharePlayerMgr;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.common.utils.BeanUtils;
import com.ddm.server.common.utils.JsonUtil;
import core.db.entity.clarkGame.PlayerBO;

/**
 * @author : xushaojun
 * create at:  2020-08-24  10:00
 * @description: 玩家创建通知
 */
@Consumer(topic = MqTopic.PLAYER_CREATE)
public class PlayerCreateNotifyConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqPLayerCreateNotifyBo mqPLayerCreateNotifyBo = (MqPLayerCreateNotifyBo) body;
        Player player = PlayerMgr.getInstance().getPlayer(mqPLayerCreateNotifyBo.getPid());
        if (player == null) {
            ThreadManager.getInstance().regThread(Thread.currentThread().getId());
            CommLogD.info("玩家创建消息通知[{}]", JsonUtil.toJson(mqPLayerCreateNotifyBo));
            SharePlayer sharePlayer = SharePlayerMgr.getInstance().getSharePlayer(mqPLayerCreateNotifyBo.getPid());
            if (sharePlayer == null || sharePlayer.getPlayerBO() == null) {
                CommLogD.warn("忽略尚未同步完成的玩家创建通知, pid:[{}]", mqPLayerCreateNotifyBo.getPid());
                return;
            }
            SharePlayerBO sharePlayerBO = sharePlayer.getPlayerBO();
            PlayerBO playerBO = new PlayerBO();
            BeanUtils.copyProperties(playerBO, sharePlayerBO);
            PlayerMgr.getInstance().regPlayer(new Player(playerBO), true);
        }

    }
}
