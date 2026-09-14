package business.rocketmq.consumer;

import BaseThread.ThreadManager;
import business.global.sharegm.ShareNodeServerMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerRoom;
import business.rocketmq.bo.MqAbsRequestBo;
import business.rocketmq.bo.MqRoomResultBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import core.network.http.proto.SData_Result;

/**
 * @author : xushaojun
 * create at:  2020-09-04  10:00
 * @description: 退出房间
 */
public abstract class BaseExitRoomConsumer {

    protected void action(Object body, Integer gameTypeId) {
        MqAbsRequestBo mqAbsRequestBo = (MqAbsRequestBo) body;
        if (mqAbsRequestBo.getGameTypeId().compareTo(gameTypeId) == 0 && ShareNodeServerMgr.getInstance().checkCurrentNode(mqAbsRequestBo.getShareNode().getIp(), mqAbsRequestBo.getShareNode().getPort())) {
            ThreadManager.getInstance().regThread(Thread.currentThread().getId());
            Player player = PlayerMgr.getInstance().getPlayer(mqAbsRequestBo.getPid());
            SData_Result result = player.getFeature(PlayerRoom.class).onExitRoom();
            MqRoomResultBo mqRoomResultBo = new MqRoomResultBo(mqAbsRequestBo, result);
            MqProducerMgr.get().send(MqTopic.ALL_HALL_BACK, mqRoomResultBo);
        }
    }
}
