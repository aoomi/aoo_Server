package business.rocketmq.consumer;

import BaseThread.ThreadManager;
import business.global.sharegm.ShareNodeServerMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerRoom;
import business.rocketmq.bo.MqAbsRequestBo;
import business.rocketmq.bo.MqRoomResultBo;
import business.rocketmq.constant.MqTopic;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.google.gson.Gson;
import core.network.http.proto.SData_Result;
import jsproto.c2s.iclass.room.CBase_EnterRoom;

/**
 * @author : xushaojun
 * create at:  2020-09-04  10:00
 * @description: 换房间的退出房间
 */
public abstract class BaseSeatRoomConsumer {

    protected void action(Object body, Integer gameTypeId) {
        MqAbsRequestBo mqAbsRequestBo = (MqAbsRequestBo) body;
        if (mqAbsRequestBo.getGameTypeId().compareTo(gameTypeId) == 0 && ShareNodeServerMgr.getInstance().checkCurrentNode(mqAbsRequestBo.getShareNode().getIp(), mqAbsRequestBo.getShareNode().getPort())) {
            ThreadManager.getInstance().regThread(Thread.currentThread().getId());
            CommLogD.info("执行进入房间开始[{}], 请求标识[{}]", mqAbsRequestBo.getGameTypeName(), mqAbsRequestBo.getRequestId());
            final CBase_EnterRoom req = new Gson().fromJson(mqAbsRequestBo.getBody(), CBase_EnterRoom.class);
            Player player = PlayerMgr.getInstance().getPlayer(mqAbsRequestBo.getPid());
            // 进入房间
            SData_Result result = player.getFeature(PlayerRoom.class).findAndSeat(req.getPosID(), req.getRoomKey(), req.getClubId(), req.getPassword(), req.isExistQuickJoin());
            MqRoomResultBo mqRoomResultBo = new MqRoomResultBo(mqAbsRequestBo, result);
            MqProducerMgr.get().send(MqTopic.BASE_SEAT_ROOM_BACK, mqRoomResultBo);
            CommLogD.info("执行进入房间结束[{}], 请求标识[{}]", mqAbsRequestBo.getGameTypeName(), mqAbsRequestBo.getRequestId());
        }
    }
}
