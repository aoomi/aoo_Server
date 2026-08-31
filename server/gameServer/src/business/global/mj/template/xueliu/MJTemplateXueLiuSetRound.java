package business.global.mj.template.xueliu;


import business.global.mj.AbsMJRoundPos;
import business.global.mj.AbsMJSetPos;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.AbsMJSetRound;
import business.global.mj.template.MJTemplateSetRound;
import cenum.mj.OpType;
import jsproto.c2s.cclass.mj.BaseMJSet_Pos;


/**
 * 基础模板 回合逻辑 每一次等待操作，都是一个round
 *
 * @author Huaxing
 */

public abstract class MJTemplateXueLiuSetRound extends MJTemplateSetRound {

    public MJTemplateXueLiuSetRound(AbsMJSetRoom set, int roundID) {
        super(set, roundID);
    }


    /**
     * 血流模式 可以一直摸牌胡牌 （抄自三副牌花麻将）
     *
     * @param preRound
     * @return
     */
    @Override
    protected boolean tryStartRoundHu(AbsMJSetRound preRound) {
        MJTemplate_XueLiuRoomSet roomSet = (MJTemplate_XueLiuRoomSet) this.set;
        //如果四家全胡了
        if (roomSet.checkXiuLiuEnd()) {
            return false;
        }
        //一炮多响，无论是否勾选，都从点炮玩家开始摸牌、打牌；
        int moPaiPos = roomSet.calcHuNextPopSetPosID();
        if (startWithGetCard(moPaiPos, true)) {
            notifyStart();
            return true;
        }
        return false;
    }

    @Override
    protected void posOpCardRet(int opPosRet, boolean isFlash) {
        int opCardID = this.set.getLastOpInfo().getLastOutCard();
        AbsMJSetPos sPos = this.set.getMJSetPos(opPosRet);
        sPos.getPosOpNotice().clearTingCardMap();
        if (this.set.getSetPosMgr().checkHuEnd() && (OpType.JiePao.equals(this.getOpType()) || OpType.Hu.equals(this.getOpType()) || OpType.QiangGangHu.equals(this.getOpType()))) {
            getSet().calcCurRoundHuPoint();
            //被抢杠的玩家要刷新 补杠的牌
            if (OpType.QiangGangHu.equals(opType)) {
                int lastOpPos = this.set.getLastOpInfo().getLastOpPos();
                MJTemplate_XueLiuSetPos beiQGHPos = (MJTemplate_XueLiuSetPos) this.set.getMJSetPos(lastOpPos);
                BaseMJSet_Pos posInfoOther = beiQGHPos.getNotify(false);
                BaseMJSet_Pos posInfoSelf = beiQGHPos.getNotify(true);
                this.set.getRoomPlayBack().playBack2Pos(-1, this.posOpCard(this.room.getRoomID(), lastOpPos, posInfoSelf, OpType.Not, opCardID, isFlash), set.getSetPosMgr().getAllPlayBackNotify());
                this.set.getRoom().getRoomPosMgr().notify2ExcludePosID(-1, this.posOpCard(this.room.getRoomID(), lastOpPos, posInfoOther, OpType.Not, opCardID, isFlash));
            }
        }
        // 刷新可胡列表
        this.refreshHuCardTypes(sPos);
        // 吃碰杠-清理牌
        if (OpType.Peng.equals(this.getOpType()) || OpType.JieGang.equals(this.getOpType())
                || OpType.Chi.equals(this.getOpType())) {
            if (OpType.Peng.equals(this.getOpType()) || OpType.JieGang.equals(this.getOpType())) {
                // 主要是跟打清空使用。 清空打牌信息
                this.cleanOutCardInfo();
            }
            this.set.getLastOpInfo().clearLastOutCard();

            if (this.checkExistClearPass()) {
                // 过手
                sPos.clearPass();
                // 漏过的玩家
                this.passLeak(opCardID, sPos.getPosID());
            }
            this.set.getSetPosMgr().clearOpTypeInfoList();
        }
        // 补杠、暗杠时候，操作牌ID == 0
        if (OpType.Gang.equals(this.getOpType()) || OpType.AnGang.equals(this.getOpType())) {
            opCardID = 0;
        }
        this.setExeOpPos(opPosRet);
        BaseMJSet_Pos posInfoOther = sPos.getNotify(false);
        BaseMJSet_Pos posInfoSelf = sPos.getNotify(true);
        this.set.getRoomPlayBack().playBack2Pos(opPosRet, this.posOpCard(this.room.getRoomID(), opPosRet, posInfoSelf, this.getOpType(), opCardID, isFlash), set.getSetPosMgr().getAllPlayBackNotify());
        this.set.getRoom().getRoomPosMgr().notify2ExcludePosID(opPosRet, this.posOpCard(this.room.getRoomID(), opPosRet, posInfoOther, this.getOpType(), opCardID, isFlash));

    }


    @Override
    protected AbsMJRoundPos nextRoundPos(int pos) {
        return new MJTemplateXueLiuRoundPos(this, pos);
    }

    @Override
    public MJTemplate_XueLiuRoomSet getSet() {
        return (MJTemplate_XueLiuRoomSet) super.getSet();
    }
}
