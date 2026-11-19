package business.rocketmq.scjymj.consumer;

import business.global.sharegm.ShareNodeServerMgr;
import business.rocketmq.bo.MqAbsRequestBo;
import business.rocketmq.constant.MqTopic;
import business.rocketmq.consumer.BaseCreateRoomConsumer;
import business.scjymj.c2s.iclass.CSCJYMJ_CreateRoom;
import cenum.PrizeType;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.google.gson.Gson;
import core.server.scjymj.SCJYMJAPP;
import jsproto.c2s.cclass.room.BaseRoomConfigure;

/**
 * @author : xushaojun
 * create at:  2020-08-19  11:17
 * @description: 红中麻将创建房间
 */
@Consumer(topic = MqTopic.BASE_CREATE_ROOM, id = SCJYMJAPP.gameTypeId)
public class SCJYMJCreateRoomConsumer extends BaseCreateRoomConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) throws ClassNotFoundException {
        MqAbsRequestBo mqAbsRequestBo = (MqAbsRequestBo) body;
        //判断游戏和请求创建节点一致
        if (mqAbsRequestBo.getGameTypeId() == SCJYMJAPP.GameType().getId() && ShareNodeServerMgr.getInstance().checkCurrentNode(mqAbsRequestBo.getShareNode().getIp(), mqAbsRequestBo.getShareNode().getPort())) {
//            CommLog.info("创建房间[{}]", mqAbsRequestBo.getGameTypeName());
            final CSCJYMJ_CreateRoom clientPack = new Gson().fromJson(mqAbsRequestBo.getBody(),
                    CSCJYMJ_CreateRoom.class);
            // 公共房间配置
            BaseRoomConfigure<CSCJYMJ_CreateRoom> configure = new BaseRoomConfigure<>(
                    PrizeType.RoomCard,
                    SCJYMJAPP.GameType(),
                    clientPack.clone());
            super.action(body, SCJYMJAPP.GameType().getId(), configure);
        }
    }
}
