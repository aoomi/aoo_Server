package business.rocketmq.consumer;

import BaseThread.ThreadManager;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerRoom;
import business.rocketmq.bo.MqAbsRequestBo;
import business.rocketmq.bo.MqRoomResultBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.room.BaseRoomConfigure;

/**
 * @author : xushaojun
 * create at:  2020-09-04  10:00
 * @description: 创建房间基础
 */
public abstract class BaseCreateRoomConsumer {

    protected void action(Object body, Integer gameTypeId, BaseRoomConfigure configure) {
        ThreadManager.getInstance().regThread(Thread.currentThread().getId());
        MqAbsRequestBo mqAbsRequestBo = (MqAbsRequestBo) body;
        CommLogD.info("执行创建房间开始[{}], 请求标识[{}]", mqAbsRequestBo.getGameTypeName(), mqAbsRequestBo.getRequestId());
        configure.setShareBaseCreateRoom(mqAbsRequestBo.getBody());
        Player player = PlayerMgr.getInstance().getPlayer(mqAbsRequestBo.getPid());
        SData_Result result = player.getFeature(PlayerRoom.class).createRoomAndConsumeCard(configure);
        MqRoomResultBo mqRoomResultBo = new MqRoomResultBo(mqAbsRequestBo, result);
        MqProducerMgr.get().send(MqTopic.BASE_CREATE_ROOM_BACK, mqRoomResultBo);
        CommLogD.info("执行创建房间结束[{}], 请求标识[{}]", mqAbsRequestBo.getGameTypeName(), mqAbsRequestBo.getRequestId());
    }
}
