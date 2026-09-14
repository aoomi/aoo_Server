package business.rocketmq.consumer;

import BaseThread.ThreadManager;
import business.global.room.ContinueRoomInfoMgr;
import business.global.sharegm.ShareNodeServerMgr;
import business.player.Player;
import business.player.PlayerMgr;
import business.player.feature.PlayerRoom;
import business.rocketmq.bo.MqAbsRequestBo;
import business.rocketmq.bo.MqRoomResultBo;
import business.rocketmq.constant.MqTopic;
import cenum.PrizeType;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.websocket.def.ErrorCode;
import com.google.gson.Gson;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.GameType;
import jsproto.c2s.cclass.room.BaseCreateRoom;
import jsproto.c2s.cclass.room.BaseRoomConfigure;
import jsproto.c2s.cclass.room.ContinueRoomInfo;
import jsproto.c2s.iclass.room.CBase_ContinueRoom;

/**
 * @author : xushaojun
 * create at:  2020-11-04  10:00
 * @description: 继续房间基础
 */
public abstract class BaseContinueRoomConsumer {

    protected <T extends BaseCreateRoom> void action(Object body, GameType gameType) {
        MqAbsRequestBo mqAbsRequestBo = (MqAbsRequestBo) body;
        //判断游戏和请求创建节点一致
        if (mqAbsRequestBo.getGameTypeId() == gameType.getId() && ShareNodeServerMgr.getInstance().checkCurrentNode(mqAbsRequestBo.getShareNode().getIp(), mqAbsRequestBo.getShareNode().getPort())) {
            ThreadManager.getInstance().regThread(Thread.currentThread().getId());
            final CBase_ContinueRoom continueRoom = new Gson().fromJson(mqAbsRequestBo.getBody(),
                    CBase_ContinueRoom.class);
            SData_Result result = null;
            ContinueRoomInfo continueRoomInfo = ContinueRoomInfoMgr.getInstance().getContinueRoomInfo(continueRoom.roomID);
            //如果找不到的话 说明已经被删除了  过了十分钟的有效时间
            if (continueRoomInfo == null) {
                result = SData_Result.make(ErrorCode.Object_IsNull, "ContinueRoomInfo Not Find", continueRoom.roomID);
            }
            //找到的话已经被使用了
            else if (continueRoomInfo.isUseFlag()) {
                result = SData_Result.make(ErrorCode.NotAllow, "ContinueRoomInfo has been used", continueRoom.roomID);
            }
            if (result == null) {
                T createRoom = (T) continueRoomInfo.getBaseRoomConfigure().getBaseCreateRoom();
                createRoom.setPaymentRoomCardType(continueRoom.continueType);
                //		 公共房间配置
                BaseRoomConfigure<? extends BaseCreateRoom> configure = new BaseRoomConfigure<>(
                        PrizeType.RoomCard,
                        gameType,
                        createRoom.clone());
                CommLogD.info("执行继续房间开始[{}], 请求标识[{}]", mqAbsRequestBo.getGameTypeName(), mqAbsRequestBo.getRequestId());
                Player player = PlayerMgr.getInstance().getPlayer(mqAbsRequestBo.getPid());
                result = player.getFeature(PlayerRoom.class).continueRoom(configure, continueRoomInfo, continueRoom);
            }
            MqRoomResultBo mqRoomResultBo = new MqRoomResultBo(mqAbsRequestBo, result);
            MqProducerMgr.get().send(MqTopic.BASE_CONTINUE_ROOM_BACK, mqRoomResultBo);
            CommLogD.info("执行继续房间结束[{}], 请求标识[{}]", mqAbsRequestBo.getGameTypeName(), mqAbsRequestBo.getRequestId());
        }
    }
}
