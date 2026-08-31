package business.rocketmq.consumer;

import BaseThread.ThreadManager;
import business.global.room.NormalRoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.RoomImpl;
import business.global.union.UnionMgr;
import business.rocketmq.bo.MqAbsRequestBo;
import business.rocketmq.bo.MqRoomResultBo;
import business.rocketmq.bo.MqUnionKickNotifyBo;
import business.rocketmq.constant.MqTopic;
import cenum.RoomTypeEnum;
import com.ddm.server.annotation.Consumer;
import com.ddm.server.common.rocketmq.MqConsumerHandler;
import com.ddm.server.common.rocketmq.MqProducerMgr;
import com.ddm.server.websocket.def.ErrorCode;
import core.network.http.proto.SData_Result;

/**
 * @author : xushaojun
 * create at:  2020-09-04  10:00
 * @description: 赛事踢出玩家
 */
@Consumer(topic = MqTopic.UNION_KICK_NOTIFY)
public class UnionKickNotifyConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqUnionKickNotifyBo bo = (MqUnionKickNotifyBo) body;
        RoomImpl roomImpl = NormalRoomMgr.getInstance().getNoneRoomByKey(bo.getRoomKey());
        if (roomImpl != null) {
            SData_Result sData_result = doKick(bo, roomImpl);
            MqRoomResultBo mqRoomResultBo = new MqRoomResultBo();
            mqRoomResultBo.setsData_result(sData_result);
            MqAbsRequestBo mqAbsRequestBo = new MqAbsRequestBo();
            mqAbsRequestBo.setOpcode("union.cunionkickroom");
            mqAbsRequestBo.setPid(bo.getPid());
            mqRoomResultBo.setMqAbsRequestBo(mqAbsRequestBo);
            MqProducerMgr.get().send(MqTopic.ALL_HALL_BACK, mqRoomResultBo);
        }

    }

    /**
     * 踢人操作
     *
     * @param bo
     * @param roomImpl
     * @return
     */
    private SData_Result doKick(MqUnionKickNotifyBo bo, RoomImpl roomImpl) {
        ThreadManager.getInstance().regThread(Thread.currentThread().getId());
        int opMinister = UnionMgr.getInstance().getUnionMemberMgr().getMinisterShare(bo.getPid(), bo.getClubId(), bo.getUnionId());
        if (opMinister <= 0) {
            return SData_Result.make(ErrorCode.UNION_NOT_EXIST_MEMBER, "UNION_NOT_EXIST_MEMBER");
        }
        int posIndex = bo.getPosIndex();
        if (posIndex < 0) {
            return SData_Result.make(ErrorCode.NotAllow, "posIndex:" + posIndex);
        }
        if (null == roomImpl) {
            return SData_Result.make(ErrorCode.NotAllow, "CUnionKickRoom not find room:" + bo.getRoomKey());
        }
        if (roomImpl.isNoneRoom() || !RoomTypeEnum.UNION.equals(roomImpl.getRoomTypeEnum())) {
            return SData_Result.make(ErrorCode.NotAllow, "CUnionKickRoom not find room:" + bo.getRoomKey());
        }

        AbsBaseRoom room = ((AbsBaseRoom) roomImpl);

        AbsRoomPos roomPos = room.getRoomPosMgr().getPosByPosID(posIndex);
        if (roomPos == null || roomPos.getClubMemberBO() == null) {
            return SData_Result.make(ErrorCode.NotAllow, "pos == null ");
        }
        long clubId = roomPos.getClubMemberBO().getClubID();

        // 被踢的人
        int kickMinister = UnionMgr.getInstance().getUnionMemberMgr().getMinisterShare(roomPos.getPid(), clubId, bo.getUnionId());


        if (kickMinister <= 0 || opMinister > kickMinister) {
            String msg = String.format("您被赛事管理@%s，从房间@%s内踢出", bo.getPlayerName(), bo.getRoomKey());
            SData_Result result = room.specialKickOut(bo.getPid(), posIndex, msg);
            if (ErrorCode.Success.equals(result.getCode())) {
                return SData_Result.make(ErrorCode.Success);
            } else {
                return SData_Result.make(result.getCode(), result.getMsg());
            }
        } else {
            return SData_Result.make(ErrorCode.UNION_MEMBER_SAME_POST_TYPE, "赛事成员相同权利");
        }
    }

}
