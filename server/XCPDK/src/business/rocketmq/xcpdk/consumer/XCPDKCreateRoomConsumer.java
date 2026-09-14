package business.rocketmq.xcpdk.consumer;

import BaseCommon.CommLog;
import BaseThread.ThreadManager;
import business.global.sharegm.ShareNodeServerMgr;
import business.xcpdk.c2s.iclass.CXCPDK_CreateRoom;
import business.rocketmq.bo.MqAbsRequestBo;
import business.rocketmq.constant.MqTopic;
import business.rocketmq.consumer.BaseCreateRoomConsumer;
import cenum.PrizeType;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.google.gson.Gson;
import core.server.xcpdk.XCPDKAPP;
import jsproto.c2s.cclass.room.BaseRoomConfigure;

/**
 * @author : xushaojun
 * create at:  2020-08-19  11:17
 * @description: 创建房间
 */
@Consumer(topic = MqTopic.BASE_CREATE_ROOM, id = XCPDKAPP.gameTypeId)
public class XCPDKCreateRoomConsumer extends BaseCreateRoomConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) throws ClassNotFoundException {
        MqAbsRequestBo mqAbsRequestBo = (MqAbsRequestBo) body;
        //判断游戏和请求创建节点一致
        if (mqAbsRequestBo.getGameTypeId() == XCPDKAPP.GameType().getId() && ShareNodeServerMgr.getInstance().checkCurrentNode(mqAbsRequestBo.getShareNode().getIp(), mqAbsRequestBo.getShareNode().getPort())) {
//            CommLog.info("创建房间[{}]", mqAbsRequestBo.getGameTypeName());
            final CXCPDK_CreateRoom clientPack = new Gson().fromJson(mqAbsRequestBo.getBody(),
                    CXCPDK_CreateRoom.class);
            // 公共房间配置
            BaseRoomConfigure<CXCPDK_CreateRoom> configure = new BaseRoomConfigure<CXCPDK_CreateRoom>(
                    PrizeType.RoomCard,
                    XCPDKAPP.GameType(),
                    clientPack.clone());
            super.action(body, XCPDKAPP.GameType().getId(), configure);
        }

    }
}
