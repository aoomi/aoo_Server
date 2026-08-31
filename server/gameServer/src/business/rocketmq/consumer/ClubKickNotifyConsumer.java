package business.rocketmq.consumer;

import BaseThread.ThreadManager;
import business.global.club.ClubMgr;
import business.global.room.NormalRoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.global.room.base.RoomImpl;
import business.rocketmq.bo.MqAbsRequestBo;
import business.rocketmq.bo.MqClubKickNotifyBo;
import business.rocketmq.bo.MqRoomResultBo;
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
 * @description: 亲友圈踢出玩家
 */
@Consumer(topic = MqTopic.CLUB_KICK_NOTIFY)
public class ClubKickNotifyConsumer implements MqConsumerHandler {


    @Override
    public void action(Object body) {
        MqClubKickNotifyBo bo = (MqClubKickNotifyBo) body;
        RoomImpl roomImpl = NormalRoomMgr.getInstance().getNoneRoomByKey(bo.getRoomKey());
        if (roomImpl != null) {
            SData_Result sData_result = doKick(bo, roomImpl);
            MqRoomResultBo mqRoomResultBo = new MqRoomResultBo();
            mqRoomResultBo.setsData_result(sData_result);
            MqAbsRequestBo mqAbsRequestBo = new MqAbsRequestBo();
            mqAbsRequestBo.setOpcode("club.cclubkickroom");
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
    private SData_Result doKick(MqClubKickNotifyBo bo, RoomImpl roomImpl) {
        ThreadManager.getInstance().regThread(Thread.currentThread().getId());
        int opMinister = ClubMgr.getInstance().getClubMemberMgr().getMinisterShare(bo.getClubId(), bo.getPid());
        if (opMinister <= 0) {
            return SData_Result.make(ErrorCode.CLUB_NOTMINISTER, "you not minister");
        }
        int posIndex = bo.getPosIndex();
        if (posIndex < 0) {
            return SData_Result.make(ErrorCode.NotAllow, "posIndex:" + posIndex);
        }
        if (null == roomImpl) {
            return SData_Result.make(ErrorCode.NotAllow, "CClubKickRoom not find room:" + bo.getRoomKey());
        }
        if (roomImpl.isNoneRoom() || !RoomTypeEnum.CLUB.equals(roomImpl.getRoomTypeEnum()) || roomImpl.getSpecialRoomId() != bo.getClubId()) {
            return SData_Result.make(ErrorCode.NotAllow, "CClubKickRoom not find room:" + bo.getRoomKey());
        }
        AbsBaseRoom room = ((AbsBaseRoom) roomImpl);
        AbsRoomPos roomPos = room.getRoomPosMgr().getPosByPosID(posIndex);
        if (roomPos == null) {
            return SData_Result.make(ErrorCode.NotAllow, "pos == null ");
        }
        int kickMinister = ClubMgr.getInstance().getClubMemberMgr().getMinister(bo.getClubId(), roomPos.getPid());
        if (kickMinister <= 0 || kickMinister > opMinister) {
            String msg = String.format("您被亲友圈管理@%s，从房间@%s内踢出", bo.getPlayerName(), bo.getRoomKey());
            SData_Result result = room.specialKickOut(bo.getPid(), posIndex, msg);
            if (ErrorCode.Success.equals(result.getCode())) {
                return SData_Result.make(ErrorCode.Success);
            } else {
                return SData_Result.make(result.getCode(), result.getMsg());
            }
        } else {
            return SData_Result.make(ErrorCode.CLUB_MEMBER_SAME_POST_TYPE, "亲友圈成员相同权利");
        }
    }

}
