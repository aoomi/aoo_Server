package business.rocketmq.consumer;

import BaseThread.ThreadManager;
import business.global.room.NormalRoomMgr;
import business.global.room.base.RoomImpl;
import business.global.union.UnionMgr;
import business.rocketmq.bo.MqAbsRequestBo;
import business.rocketmq.bo.MqRoomResultBo;
import business.rocketmq.bo.MqUnionDissolveRoomNotifyBo;
import business.rocketmq.constant.MqTopic;
import cenum.RoomTypeEnum;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.websocket.def.ErrorCode;
import core.network.http.proto.SData_Result;
import jsproto.c2s.cclass.club.Club_define;

/**
 * @author : xushaojun
 * create at:  2020-09-04  10:00
 * @description: 赛事解散房间
 */
@Consumer(topic = MqTopic.UNION_DISSOLVE_ROOM_NOTIFY)
public class UnionDissolveRoomNotifyConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqUnionDissolveRoomNotifyBo bo = (MqUnionDissolveRoomNotifyBo) body;
        RoomImpl roomImpl = NormalRoomMgr.getInstance().getNoneRoomByKey(bo.getRoomKey());
        if (roomImpl != null) {
            SData_Result sData_result = doDissolveRoom(bo, roomImpl);
            MqRoomResultBo mqRoomResultBo = new MqRoomResultBo();
            mqRoomResultBo.setsData_result(sData_result);
            MqAbsRequestBo mqAbsRequestBo = new MqAbsRequestBo();
            mqAbsRequestBo.setOpcode("union.uniondissolveroom");
            mqAbsRequestBo.setPid(bo.getPid());
            mqRoomResultBo.setMqAbsRequestBo(mqAbsRequestBo);
            MqProducerMgr.get().send(MqTopic.ALL_HALL_BACK, mqRoomResultBo);
        }

    }

    /**
     * 解散房间操作
     *
     * @param bo
     * @param room
     * @return
     */
    private SData_Result doDissolveRoom(MqUnionDissolveRoomNotifyBo bo, RoomImpl room) {
        ThreadManager.getInstance().regThread(Thread.currentThread().getId());
        int minister = UnionMgr.getInstance().getUnionMemberMgr().getMinisterShare(bo.getPid(), bo.getClubId(), bo.getUnionId());
        if (minister <= 0) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "UNION_NOT_EXIST_MEMBER");
        }
        if (null == room) {
            return SData_Result.make(ErrorCode.ExitROOM_ERROR_NOTFINDROOM, "CUnionDissolveRoom not find roomKey :" + bo.getRoomKey());
        }
        if (room.getSpecialRoomId() != bo.getUnionId() || !RoomTypeEnum.UNION.equals(room.getRoomTypeEnum())) {
            return SData_Result.make(ErrorCode.ExitROOM_ERROR_NOTFINDROOM, "CUnionDissolveRoom not find roomKey :" + bo.getRoomKey());
        }

//		房间@房间ID已被亲友圈管理@管理名称 解散
//		房间@房间ID已被群主@群主名称 解散
        String ministerName = minister == Club_define.Club_MINISTER.Club_MINISTER_CREATER.value() ? "创建者" : "管理员";
        String msg = String.format("房间@%s已被赛事%s@%s 解散", bo.getRoomKey(), ministerName, bo.getPlayerName());
        SData_Result result = room.specialDissolveRoom(bo.getUnionId(), RoomTypeEnum.UNION, minister, msg);
        if (ErrorCode.Success.equals(result.getCode())) {
            CommLogD.info("CUnionDissolveRoom Success Pid:{},UnionId:{},clubId:{},RoomKey:{},minister:{},msg:{}", bo.getPid(), bo.getUnionId(), bo.getClubId(), bo.getRoomKey(), minister, msg);
            return result;
        } else {
            return SData_Result.make(result.getCode(), result.getMsg());
        }
    }

}
