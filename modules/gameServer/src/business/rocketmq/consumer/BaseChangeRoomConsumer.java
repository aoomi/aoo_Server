package business.rocketmq.consumer;

import BaseThread.ThreadManager;
import business.global.sharegm.ShareNodeServerMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerRoom;
import business.rocketmq.bo.MqAbsRequestBo;
import business.rocketmq.bo.MqChangeRoomBo;
import business.rocketmq.bo.MqRoomResultBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.websocket.def.ErrorCode;
import core.network.http.proto.SData_Result;

/**
 * @author : xushaojun
 * create at:  2020-09-04  10:00
 * @description: 换房间的退出房间基础类
 */
public abstract class BaseChangeRoomConsumer {

    protected void action(Object body, Integer gameTypeId) {
        MqChangeRoomBo mqChangeRoomBo = (MqChangeRoomBo) body;
        if (mqChangeRoomBo.getExitGameTypeId().compareTo(gameTypeId) == 0 && ShareNodeServerMgr.getInstance().checkCurrentNode(mqChangeRoomBo.getExitShareNode().getIp(), mqChangeRoomBo.getExitShareNode().getPort())) {
            ThreadManager.getInstance().regThread(Thread.currentThread().getId());
            Player player = PlayerMgr.getInstance().getPlayer(mqChangeRoomBo.getPid());
            SData_Result result = player.getFeature(PlayerRoom.class).onExitRoom();
            MqAbsRequestBo mqAbsRequestBo = new MqAbsRequestBo(mqChangeRoomBo.getPid(), mqChangeRoomBo.getEnterGameTypeName(), mqChangeRoomBo.getEnterGameTypeId(), mqChangeRoomBo.getBody(), mqChangeRoomBo.getOpcode(), mqChangeRoomBo.getEnterShareNode());
            mqAbsRequestBo.setShareNodeFrom(mqChangeRoomBo.getShareNodeFrom());
            //退出房间正常就进入新房间
            if (ErrorCode.Success.equals(result.getCode())) {
                MqProducerMgr.get().send(MqTopic.BASE_ENTER_ROOM + mqChangeRoomBo.getEnterGameTypeId(), mqAbsRequestBo);
            } else {//异常就返回信息给大厅
                MqRoomResultBo mqRoomResultBo = new MqRoomResultBo(mqAbsRequestBo, result);
                MqProducerMgr.get().send(MqTopic.ALL_HALL_BACK, mqRoomResultBo);
            }
        }
    }
}
