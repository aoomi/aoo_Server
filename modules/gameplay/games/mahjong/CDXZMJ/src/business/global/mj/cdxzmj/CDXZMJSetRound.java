package business.global.mj.cdxzmj;


import business.cdxzmj.c2s.iclass.SCDXZMJ_PosOpCard;
import business.cdxzmj.c2s.iclass.SCDXZMJ_StartRound;
import business.global.mj.AbsMJRoundPos;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.set.MJOpCard;
import business.global.mj.set.MJTemplate_OpCard;
import business.global.mj.template.MJTemplateRoomSet;
import business.global.mj.template.MJTemplateRoundPos;
import business.global.mj.template.xueZhan.MJTemplateXueZhanSetRound;
import cenum.mj.OpType;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.handler.requset.WebSocketRequestDelegate;
import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;
import java.util.Objects;


/**
 * 基础模板 回合逻辑 每一次等待操作，都是一个round
 *
 * @author Huaxing
 */

public class CDXZMJSetRound extends MJTemplateXueZhanSetRound {

    public CDXZMJSetRound(AbsMJSetRoom set, int roundID) {
        super(set, roundID);
    }

    /**
     * 位置操作牌
     */
    @Override
    public <T> BaseSendMsg posOpCard(long roomID, int pos, T set_Pos, OpType opType, int opCard, boolean isFlash) {
        return SCDXZMJ_PosOpCard.make(roomID, pos, set_Pos, opType, opCard, isFlash);
    }

    @Override
    protected <T> BaseSendMsg startRound(long roomID, T room_SetWait) {
        return SCDXZMJ_StartRound.make(roomID, room_SetWait);
    }


    @Override
    protected AbsMJRoundPos nextRoundPos(int pos) {
        return new CDXZMJRoundPos(this, pos);
    }


    @Override
    protected boolean autoOutCard(int sec) {

        if (sec - this.startTime < 1) {
            return false;
        }
        MJTemplateRoundPos roundPos;
        CDXZMJSetPos sPos;
        MJTemplateRoomSet roomSet;
        int cardID;
        for (int posID = 0; posID < this.room.getPlayerNum(); posID++) {
            roundPos = (MJTemplateRoundPos) this.roundPosDict.get(posID);
            if (null == roundPos) {
                continue;
            }
            sPos = (CDXZMJSetPos) roundPos.getPos();
            if (null == sPos) {
                continue;
            }
            roomSet = (MJTemplateRoomSet) sPos.getSet();
            if (null == roomSet) {
                continue;
            }
            List<OpType> opList = roundPos.getRecieveOpTypes();
            if (null == opList || opList.size() <= 0) {
                continue;
            }
            if (getStartTime() + getRoom().wanFa_ChangeCardType_Time() <= CommTime.nowSecond() && opList.contains(OpType.HuanSanZhang) && sPos.getOpCardList().isEmpty()) {
                this.opCard(new WebSocketRequestDelegate(), roundPos.getOpPos(), OpType.HuanSanZhang, MJTemplate_OpCard.OpCard(0, sPos.getFirstChangeCardList()));
                continue;
            }
            if (((CDXZMJSetCard) getSet().getSetCard()).isMustHu()) {
                if (opList.contains(OpType.Hu)) {
                    this.opCard(new WebSocketRequestDelegate(), roundPos.getOpPos(), OpType.Hu, MJOpCard.OpCard(sPos.getHandCard().getCardID()));
                } else if (opList.contains(OpType.JiePao)) {
                    this.opCard(new WebSocketRequestDelegate(), roundPos.getOpPos(), OpType.JiePao, MJOpCard.OpCard(this.set.getLastOpInfo().getLastOutCard()));
                } else if (opList.contains(OpType.QiangGangHu)) {
                    this.opCard(new WebSocketRequestDelegate(), roundPos.getOpPos(), OpType.QiangGangHu, MJOpCard.OpCard(this.set.getLastOpInfo().getLastOpCard()));
                }
                return false;
            }
            if (opList.contains(OpType.Gang) || opList.contains(OpType.JieGang) || opList.contains(OpType.AnGang)) {
                continue;
            }

            if (Objects.nonNull(sPos.getAutoHu()) && sPos.getAutoHu() == 1) {
                if (opList.contains(OpType.Hu)) {
                    this.opCard(new WebSocketRequestDelegate(), roundPos.getOpPos(), OpType.Hu, MJOpCard.OpCard(sPos.getHandCard().getCardID()));
                } else if (opList.contains(OpType.JiePao)) {
                    this.opCard(new WebSocketRequestDelegate(), roundPos.getOpPos(), OpType.JiePao, MJOpCard.OpCard(this.set.getLastOpInfo().getLastOutCard()));
                } else if (opList.contains(OpType.QiangGangHu)) {
                    this.opCard(new WebSocketRequestDelegate(), roundPos.getOpPos(), OpType.QiangGangHu, MJOpCard.OpCard(this.set.getLastOpInfo().getLastOpCard()));
                }
            }
            if (Objects.nonNull(sPos.getAutoOut()) && sPos.getAutoOut() == 1 && opList.contains(OpType.Out)) {
                this.opCard(new WebSocketRequestDelegate(), roundPos.getOpPos(), OpType.Out, MJOpCard.OpCard(sPos.getSetPosRobot().getAutoCard2()), true);
            }
        }
        return false;
    }

}	
