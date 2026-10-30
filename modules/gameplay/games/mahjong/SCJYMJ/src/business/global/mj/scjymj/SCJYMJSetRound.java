package business.global.mj.scjymj;

import business.global.karmicmj.KarMJSetRound;
import business.global.mj.*;
import business.global.mj.manage.MJFactory;
import business.global.mj.set.MJOpCard;
import business.scjymj.c2s.cclass.SCJYMJRoom_RoundPos;
import business.scjymj.c2s.iclass.SSCJYMJ_PosOpCard;
import business.scjymj.c2s.iclass.SSCJYMJ_StartRound;
import cenum.mj.OpType;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.handler.requset.WebSocketRequestDelegate;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.mj.BaseMJRoom_SetRound;
import jsproto.c2s.cclass.mj.BaseMJSet_Pos;
import jsproto.c2s.cclass.mj.NextOpType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 江苏扬中麻将 回合逻辑 每一次等待操作，都是一个round
 *
 * @author Huaxing
 */

public class SCJYMJSetRound extends KarMJSetRound {

    AbsMJSetRound firstRound;

    public SCJYMJSetRound(AbsMJSetRoom set, int roundID) {
        super(set, roundID);
    }

    @Override
    public boolean update(int sec) {
        // 已经结束
        if (this.endTime != 0) {
            if (this.endTime >= this.startTime) {
                return true;
            }
            return false;
        }
        // 自动打牌
        return this.autoOutCard(sec);
    }

    /**
     * 自动打牌
     *
     * @param sec
     * @return
     */
    @Override
    protected boolean autoOutCard(int sec) {
        if (sec - this.startTime < 1) {
            return false;
        }
        SCJYMJRoundPos roundPos;
        SCJYMJSetPos sPos;
        for (int posID = 0; posID < this.room.getPlayerNum(); posID++) {
            roundPos = (SCJYMJRoundPos) this.roundPosDict.get(posID);
            if (null == roundPos) {
                continue;
            }
            sPos = (SCJYMJSetPos) roundPos.getPos();
            if (null == sPos || sPos.getRoomPos().isTrusteeship()) {
                continue;
            }

            List<OpType> opList = roundPos.getRecieveOpTypes();
            if (null == opList || opList.size() <= 0) {
                continue;
            }
            if (sPos.isTing()) {
                int cardID = 0;
                if (opList.contains(OpType.Hu)) {
                    continue;
                } else if (opList.contains(OpType.JiePao)) {
                    continue;
                } else if (opList.contains(OpType.AnGang) && sPos.getHandCard() != null) {
                    continue;
                } else if (opList.contains(OpType.JieGang)) {
                    continue;
                } else if (opList.contains(OpType.Out)) {
                    if (null != sPos.getHandCard()) {
                        cardID = sPos.getHandCard().cardID;
                    } else {
                        continue;
                    }
                    AbsMJSetRound preRound = set.getPreRound();
                    if (null != preRound && 1 == preRound.getRoundID() && (preRound.getOpType() == null || preRound.getOpType() == OpType.Pass || preRound.getOpType() == OpType.KouTing)) {
                        continue;
                    }
                    this.opCard(new WebSocketRequestDelegate(), roundPos.getOpPos(), OpType.Out, MJOpCard.OpCard(cardID));
                }
            }
        }
        return false;
    }

    /**
     * 检查报听
     */
    private boolean checkBaoTing() {
        this.set.getPosDict().values().forEach(k -> k.calcHuFan());
        SCJYMJSetPos setPos;
        boolean hasTing = false;
        for (int i = 0; i < this.set.getPlayerNum(); i++) {
            int nextPos = (this.set.getDPos() + i) % this.set.getPlayerNum();
            setPos = (SCJYMJSetPos) this.set.getMJSetPos(nextPos);
            if (null == setPos){
                continue;
            }
            if (13 == setPos.getPrivateCards().size() && null != setPos.getHandCard()) {
                List<MJCard> allCards = setPos.allCards();
                boolean canBaoTing = false;
                if (14 == allCards.size()) {
                    for (MJCard card:allCards) {
                        List<MJCard> tempCards = new ArrayList<>(allCards);
                        tempCards.remove(card);
                        List<Integer> tingCards = MJFactory.getTingCard(setPos.getmActMrg()).checkTingCard(setPos, tempCards);
                        if (tingCards.size() > 0) {
                            canBaoTing = true;
                            break;
                        }
                    }
                }
                if (!canBaoTing) {
                    continue;
                }
            } else if (13 == setPos.getPrivateCards().size()) {
                if (setPos.sizeHuCardTypes() <= 0) {
                    continue;
                }
            }

            hasTing = true;
            AbsMJRoundPos tmPos = new SCJYMJRoundPos(this, nextPos);
            tmPos.addOpType(OpType.KouTing);
            tmPos.addOpType(OpType.Pass);
            this.roundPosDict.put(tmPos.getOpPos(), tmPos);
            ((SCJYMJSetPosMgr) tmPos.getSetPosMgr()).addBaoTingInfo(setPos.getPosID());
        }
        return hasTing;
    }

    // 尝试开始回合, 如果失败，则set结束
    public boolean tryStartRound() {

        AbsMJSetRound preRound = getPreRound(); // 前一个可参考的操作round
        // 第一次，庄家作为操作者，抓牌，等待出牌
        if (null == preRound) {
            if (null == this.set.getCard(this.set.getDPos(), true)) {
                return false;
            }
            boolean hasTing = checkBaoTing();
            if (!hasTing) {
                tryEndRound(false);
            }
            this.notifyStart();
            return true;
        }
        if (preRound.getRoundID() == 1) {
            if (!MJRoundPos(this.set.getDPos())) {
                return false;
            }
            this.notifyStart();
            return true;
        }
//        if (preRound.getRoundID() == 2 && preRound.getOpType() == OpType.Out) {
//            firstRound = preRound;
//            boolean hasTing = checkBaoTing(preRound.getExeOpPos());
//            if (!hasTing) {
//                tryEndRound(false);
//            }
//            this.notifyStart();
//            return true;
//        }
//        if (preRound.getRoundID() == 3 && (preRound.getOpType() == null || preRound.getOpType() == OpType.Pass || preRound.getOpType() == OpType.KouTing)) {
//            // 出牌对应的接手操作
//            if (((SCJYMJSetRound) preRound).firstRound.checkExistNextRoundOp()) {
//                // 检查下回合操作位置
//                return this.checkNextRoundOpPos(((SCJYMJSetRound) preRound).firstRound);
//            }
//            // 无人接手
//            else {
//                AbsMJSetPos aPos = null;
//                for (int i = 1; i < this.set.getPlayerNum(); i++) {
//                    // 只能顺序的抓牌，打牌
//                    int nextPos = (set.getDPos() + i) % this.room.getPlayerNum();
//                    aPos = this.set.getMJSetPos(nextPos);
//                    if (null == aPos || aPos.isHu()) {
//                        continue;
//                    }
//                    if (!startWithGetCard(nextPos, true)) {
//                        return false;
//                    }
//                    notifyStart();
//                    return true;
//                }
//                notifyStart();
//            }
//        }

        // 上轮出牌
        if (preRound.getOpType() == OpType.Out || preRound.getOpType() == OpType.KouTing) {
            return tryStartRoundOut(preRound);
        }
        // 上一轮接牌， 本轮继续出牌
        if (preRound.getOpType() == OpType.Peng) {
            return tryStartRoundPeng(preRound);
        }

        // 上一轮明杠，等抢胡，或者继续抓牌
        if (preRound.getOpType() == OpType.Gang || preRound.getOpType() == OpType.JieGang) {
            return tryStartRoundGang(preRound);
        }

        // 上一轮暗杠，本轮继续抓牌
        if (preRound.getOpType() == OpType.AnGang || preRound.getOpType() == OpType.TianGang) {
            return tryStartRoundAnGang(preRound);
        }

        // 上一轮接牌， 本轮继续出牌
        if (preRound.getOpType() == OpType.Chi) {
            return tryStartRoundChi(preRound);
        }

        // 上一轮放弃接牌
        if (preRound.getOpType() == OpType.Pass) {
            return tryStartRoundPass(preRound);
        }

        // 尝试开始其他回合
        return tryStartRoundOther(preRound);
    }

    /**
     * 开始本回合,并摸牌
     *
     * @param pos
     * @param isNormalMo
     * @return
     */
    @Override
    public boolean startWithGetCard(int pos, boolean isNormalMo) {
        // 抓牌
        if (null == this.set.getMJSetPos(pos).getHandCard()) { // 作弊情况下，已经有手牌
            if (null == this.set.getCard(pos, isNormalMo)) {
                return false;
            }
        }
//        if (this.set.isAtFirstHu()) {
//            // 定缺：在发完牌后选择一门不要的花色即为定缺；
//            //  3房牌才定缺，2房不用定缺；
//            //  对局中不可更改定缺；
//            //  选择定缺后玩家头像加定缺花色的图标；
//            //  出完定缺的花色牌之前，其他牌锁定不能出；
//            //  摸到定缺花色的牌时只能打出，其他牌锁定不能出。
//            // 3房牌必须缺一门才可以胡牌，即胡牌的时候不能有三种花色的牌；
//            // 游戏开始前，玩家需要选择一门要打缺的花色来，定缺后玩家必须在打完所持有的已定缺花色的牌之后，才可以打出其他花色的牌。
//            this.set.setAtFirstHu(false);
//            if (SCJYMJRoomEnum.SCJYMJFangZhang.PAI_3.equals(((SCJYMJRoom) this.room).getFangZhang())) {
//                this.set.getPosDict().values().forEach(k -> baoTing(k.getPosID()));
//                return true;
//            } else {
//                if (tryStartBaoTing()) {
//                    return true;
//                }
//            }
//        }
        return MJRoundPos(pos);
    }

    /**
     * 定缺
     *
     * @param pos
     * @return
     */
    private void baoTing(int pos) {
        AbsMJRoundPos tmPos = this.nextRoundPos(pos);
        tmPos.addOpType(Arrays.asList(OpType.Ting));
        this.roundPosDict.put(tmPos.getOpPos(), tmPos);
    }

    /**
     * 下回合操作者的位置
     *
     * @param pos
     * @return
     */
    @Override
    protected AbsMJRoundPos nextRoundPos(int pos) {
        return new SCJYMJRoundPos(this, pos);
    }

    /**
     * 位置操作牌
     */
    @Override
    public <T> BaseSendMsg posOpCard(long roomID, int pos, T set_Pos, OpType opType, int opCard, boolean isFlash) {
        return SSCJYMJ_PosOpCard.make(roomID, pos, set_Pos, opType, opCard, isFlash);
    }

    /**
     * 开始当前回合通知
     */
    @Override
    protected <T> BaseSendMsg startRound(long roomID, T room_SetWait) {
        return SSCJYMJ_StartRound.make(roomID, room_SetWait);
    }

    /**
     * 尝试开始其他回合 如果 没有其他特殊回合 默认返回 false 否则 对其他特殊操作类型进行操作检查
     */
    @Override
    protected boolean tryStartRoundOtherKar(AbsMJSetRound preRound) {
        return false;
    }

    /**
     * 报听
     *
     * @return
     */
    private boolean tryStartBaoTing() {
//        return checkExistNotTing();
        OpType opType = OpType.KouTing;
        this.set.getSetPosMgr().checkOpType(this.set.getDPos(), 0, opType);
        NextOpType nOpType = this.set.getSetPosMgr().exeCardAction(opType);
        if (null != nOpType) {
            for (int posID : nOpType.getPosOpTypeListMap().keySet()) {
                AbsMJRoundPos nextPos = this.nextRoundPos(posID);
                if (nOpType.getPosOpTypeListMap().containsKey(posID)) {
                    nextPos.addOpType(nOpType.getPosOpTypeListMap().get(posID));
                }
                nextPos.addOpType(OpType.Pass);
                this.roundPosDict.put(nextPos.getOpPos(), nextPos);
            }
            return true;
        }
        return false;
    }

    /**
     * @param preRound
     * @param opType
     * @return
     */
    private boolean tryStart(AbsMJSetRound preRound, OpType opType) {
        this.set.getSetPosMgr().checkOpType(this.set.getDPos(), 0, opType);
        NextOpType nOpType = this.set.getSetPosMgr().exeCardAction(opType);
        if (null != nOpType) {
            for (int posID : nOpType.getPosOpTypeListMap().keySet()) {
                AbsMJRoundPos nextPos = this.nextRoundPos(posID);
                if (nOpType.getPosOpTypeListMap().containsKey(posID)) {
                    nextPos.addOpType(nOpType.getPosOpTypeListMap().get(posID));
                }
                nextPos.addOpType(OpType.Pass);
                this.roundPosDict.put(nextPos.getOpPos(), nextPos);
            }
            this.waitDealRound = preRound; // 本轮，接手处理 preRound
            notifyStart();
            return true;
        }
        return tryStartBaoTing();
    }

    protected boolean checkExistCleanPass() {
        return true;
    }


    /**
     * 获取本轮信息
     *
     * @param pos 位置
     * @return
     */

    @Override
    public BaseMJRoom_SetRound getNotify_RoundInfo(int pos) {
        ret = new BaseMJRoom_SetRound();
        ret.setWaitID(this.roundID);
        ret.setStartWaitSec(this.startTime);
        for (AbsMJRoundPos roundPos : this.roundPosDict.values()) {
            if (roundPos.getOpType() != null) {
                continue;
            }
            // 自己 或 公开
            if (pos == roundPos.getOpPos() || roundPos.isPublicWait()) {
                SCJYMJRoom_RoundPos data = new SCJYMJRoom_RoundPos();
                boolean isSelf = pos == roundPos.getOpPos();
                data.setOpList(roundPos.getRecieveOpTypes());
                data.setChiList(roundPos.getPos().getPosOpNotice().getChiList());
                data.setLastOpCard(roundPos.getLastOutCard());
                data.setWaitOpPos(roundPos.getOpPos());
                data.setTingCardMap(isSelf ? roundPos.getPos().getPosOpNotice().getTingCardMap() : null);
                data.setBuChuList(isSelf ? roundPos.getPos().getPosOpNotice().getBuNengChuList() : null);
                ret.addOpPosList(data);
                roundPos.getPos().getPosOpRecord().setOpList(data.getOpList());
                this.set.getLastOpInfo().setLastShotTime(CommTime.nowSecond());
            }
        }
        return ret;
    }

    /**
     * 位置操作牌
     *
     * @param opPosRet 操作位置
     * @param isFlash  是否动画
     */
    @Override
    protected void posOpCardRet(int opPosRet, boolean isFlash) {
        int opCardID = this.set.getLastOpInfo().getLastOutCard();
        AbsMJSetPos sPos = this.set.getMJSetPos(opPosRet);
        sPos.getPosOpNotice().clearTingCardMap();
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
            if (this.checkExistCleanPass()) {
                if (sPos.getPosOpRecord().getOpList().contains(OpType.JiePao) || sPos.getPosOpRecord().getOpList().contains(OpType.QiangGangHu)) {
                    if (sPos.sizePrivateCard() <= 2) {
                        sPos.clearPass();
                    } else {
                        sPos.getPosOpRecord().clearOpCardType();
                    }
                } else {
                    // 过手
                    sPos.clearPass();
                }
            }
            this.set.getSetPosMgr().clearOpTypeInfoList();
        }
        if (OpType.JiePao.equals(this.getOpType())) {
            this.set.getLastOpInfo().clearLastOutCard();
        }
        // 补杠、暗杠时候，操作牌ID == 0
        if (OpType.Gang.equals(this.getOpType()) || OpType.AnGang.equals(this.getOpType())) {
            opCardID = 0;
        }
        this.setExeOpPos(opPosRet);
        BaseMJSet_Pos posInfoOther = sPos.getNotify(false);
        BaseMJSet_Pos posInfoSelf = sPos.getNotify(true);
        SSCJYMJ_PosOpCard opCard = (SSCJYMJ_PosOpCard) this.posOpCard(this.room.getRoomID(), opPosRet, posInfoSelf,
                this.getOpType(), opCardID, isFlash);
        this.set.getRoomPlayBack().playBack2Pos(opPosRet, opCard, set.getSetPosMgr().getAllPlayBackNotify());
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            if (i == opPosRet) {
                continue;
            }
            SSCJYMJ_PosOpCard otherOpCard = (SSCJYMJ_PosOpCard) this.posOpCard(this.room.getRoomID(), opPosRet,
                    posInfoOther, this.getOpType(), opCardID, isFlash);
            this.room.getRoomPosMgr().notify2Pos(i, otherOpCard);
        }
    }

    @Override
    protected boolean checkExistClearPass() {
        return true;
    }

    /**
     * 检查是否直接过
     *
     * @return
     */
    @Override
    protected boolean checkPass() {
        SCJYMJSetPos aPos;
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            aPos = (SCJYMJSetPos) set.getMJSetPos(i);
            if (aPos.isHu()) {
                continue;
            }
            if (aPos.getHandCard() != null) {
                AbsMJRoundPos nextPos = this.nextRoundPos(i);
                if (nextPos.getPos().sizeOutCardIDs() <= 0) {
                    nextPos.getPos().clearPass();
                    nextPos.addOpType(nextPos.getPos().recieveOpTypes());
                } else {
                    nextPos.addOpType(OpType.Out);
                }
                this.roundPosDict.put(nextPos.getOpPos(), nextPos);
                notifyStart();
                return true;
            }
        }
        return false;
    }

    /**
     * 下位置操作类型
     *
     * @param nextPos
     * @return
     */
    @Override
    public AbsMJRoundPos nextPosOpType(AbsMJRoundPos nextPos) {
        if (nextPos.getPos().checkOpType(0, OpType.Ting)) {
            nextPos.addOpType(OpType.Ting);
        }
        if (this.room.RoomCfg(SCJYMJRoomEnum.SCJYMJCfg.PengGang)) {
            nextPos.addOpType(nextPos.getPos().recieveOpTypes());
            this.roundPosDict.put(nextPos.getOpPos(), nextPos);
        } else {
            // 碰后只能出牌
            nextPos.addOpType(OpType.Out);
        }
        return nextPos;
    }

    /**
     * 机器人操作
     *
     * @param posID
     */
    public void RobothandCrad(int posID) {
        new SCJYMJRobotOpCard(this).RobothandCrad(posID);
    }

}
