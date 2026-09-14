package business.global.mj.template;


import business.global.mj.*;
import business.global.mj.manage.MJFactory;
import business.global.mj.op.*;
import business.global.mj.robot.MJTemplateRobotOpCard;
import business.global.mj.set.MJOpCard;
import business.global.mj.set.MJTemplate_OpCard;
import cenum.mj.MJOpCardError;
import cenum.mj.OpType;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.ddm.server.websocket.handler.requset.WebSocketRequestDelegate;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.mj.BaseMJRoom_RoundPos;
import jsproto.c2s.cclass.mj.BaseMJRoom_SetRound;
import jsproto.c2s.cclass.mj.NextOpType;
import jsproto.c2s.cclass.mj.template.MJTemplateRoom_SetRound;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * 基础模板 回合逻辑 每一次等待操作，都是一个round
 *
 * @author Huaxing
 */

public abstract class MJTemplateSetRound extends AbsMJSetRound {
    public boolean isHdly = false;//是否海底捞月

    public MJTemplateSetRound(AbsMJSetRoom set, int roundID) {
        super(set, roundID);
    }

    @Override
    public int opCard(WebSocketRequest request, int opPos, OpType opType, MJOpCard mOpCard) {
        return opCard(request, opPos, opType, mOpCard, false);
    }


    /**
     * @param request
     * @param opPos
     * @param opType
     * @param mOpCard
     * @param isFlash
     * @return
     */
    public synchronized int opCard(WebSocketRequest request, int opPos, OpType opType, MJOpCard mOpCard, boolean isFlash) {
        if (this.getEndTime() > 0) {
            request.error(ErrorCode.NotAllow, "end Time opPos has no round power");
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        AbsMJRoundPos pos = this.roundPosDict.get(opPos);
        if (null == pos) {
            request.error(ErrorCode.NotAllow, "opPos has no round power");
            return MJOpCardError.ROUND_POS_ERROR.value();
        }
        int opCardRet = pos.op(request, opType, mOpCard);
        if (opCardRet >= 0) {
            // 位置操作牌
            this.posOpCardRet(opCardRet, isFlash);
        }
        return opCardRet;
    }

    /**
     * 机器人操作
     *
     * @param posID
     */
    @Override
    public void RobothandCrad(int posID) {

        if (this.getEndTime() > 0) {
            return;
        }
        if (this.getRoundPosDict().containsKey(posID)) {
            new MJTemplateRobotOpCard(this).RobothandCrad(posID);
        }
    }

    /**
     * 开始本回合,并摸牌
     *
     * @param pos
     * @param isNormalMo
     * @return
     */
    public boolean startWithGetCard(int pos, boolean isNormalMo) {
        if (set.isAtFirstHu()) {
            set.setAtFirstHu(false);
            //庄家起手14张
            if (null == this.set.getMJSetPos(pos).getHandCard()) {
                if (null == this.set.getCard(pos, isNormalMo)) {
                    return false;
                }
            }
            //起手补花
            atFirstHuBuHua();
            //开金
            if (atFirstHuKaiJin()) {
                this.set.getPosDict().values().forEach(k -> {
                    k.calcHuFan();
                    k.sortCards();
                });
                this.set.sendSetPosCard();
            }
            //换张/换牌
            if (atFirstHuChangeCard()) {
                return true;
            }
            //定缺
            if (atFirstHuDingQue()) {
                return true;
            }
            //起手听牌
            if (atFirstHuTianTing()) {
                return true;
            }
        }
        //海底捞月
        if (startWithHaiDiLaoYue(pos, isNormalMo)) {
            return true;
        }
        //没牌了
        if (getSet().getSetCard().isPopCardNull()) {
            return false;
        }
        // 抓牌
        // 作弊情况下，已经有手牌
        if (null == this.set.getMJSetPos(pos).getHandCard()) {
            if (null == this.set.getCard(pos, isNormalMo)) {
                return false;
            }
        }
        return MJRoundPos(pos);
    }

    /**
     * 开金
     * 无,本金是金3张，本金是金4张，退金4张，进金4张，本金+进金7张，本金+进金8张，
     * * 本金+退金7张，本金+退金8张，进金+退金8张，进金+退金+本金11张，进金+退金+本金12张
     *
     * @return
     */
    public boolean atFirstHuKaiJin() {

        switch (getRoom().wanFa_KaiJin()) {
            case BENJIN_3ZHANG://本金是金3张
                MJFactory.getOpCard(KaiJinImpl.class).checkOpCard(this.set.getMJSetPos(this.set.getDPos()), 0);
                break;
            case BENJIN_4ZHANG://本金是金4张
                MJFactory.getOpCard(KaiJinBenJin4ZhangImpl.class).checkOpCard(this.set.getMJSetPos(this.set.getDPos()), 0);
                break;
            case JINJIN_4ZHANG://进金4张
                MJFactory.getOpCard(KaiJinJJ4Impl.class).checkOpCard(this.set.getMJSetPos(this.set.getDPos()), 0);
                break;
            case BACKJIN_4ZHANG://退金4张
                MJFactory.getOpCard(KaiJinBackJin4ZhangImpl.class).checkOpCard(this.set.getMJSetPos(this.set.getDPos()), 0);
                break;
            case BENJIN_ADD_JINJIN_7ZHANG://本金+进金7张
                MJFactory.getOpCard(KaiJinBenJinAddJinJin7ZhangImpl.class).checkOpCard(this.set.getMJSetPos(this.set.getDPos()), 0);
                break;
            case BENJIN_ADD_JINJIN_8ZHANG://本金+进金8张
                MJFactory.getOpCard(KaiJinBenJinAddJinJin8ZhangImpl.class).checkOpCard(this.set.getMJSetPos(this.set.getDPos()), 0);
                break;
            case BENJIN_ADD_BACKJIN_7ZHANG://本金+退金7张
                MJFactory.getOpCard(KaiJinBenJinAddBackJin7ZhangImpl.class).checkOpCard(this.set.getMJSetPos(this.set.getDPos()), 0);
                break;
            case BENJIN_ADD_BACKJIN_8ZHANG://本金+退金8张
                MJFactory.getOpCard(KaiJinBenJinAddBackJin8ZhangImpl.class).checkOpCard(this.set.getMJSetPos(this.set.getDPos()), 0);
                break;
            case BACK_JIN_ADD_JIN_JIN_8ZHANG://进金+退金8张
                MJFactory.getOpCard(KaiJinJJ3Impl.class).checkOpCard(this.set.getMJSetPos(this.set.getDPos()), 0);
                break;
            case BACK_JIN_ADD_BENJIN_ADD_JIN_JIN_11ZHANG://进金+退金+本金11张
                MJFactory.getOpCard(KaiJinBackJinAddBenJinAddJinJin11ZhangImpl.class).checkOpCard(this.set.getMJSetPos(this.set.getDPos()), 0);
                break;
            case BACK_JIN_ADD_BENJIN_ADD_JIN_JIN_12ZHANG://进金+退金+本金12张
                MJFactory.getOpCard(KaiJinBackJinAddBenJinAddJinJin12ZhangImpl.class).checkOpCard(this.set.getMJSetPos(this.set.getDPos()), 0);
                break;
            case JIN1_ADD_JIN2_8ZHANG://翻开一张牌，该牌的顺一位和二位做为混牌 8张；
                MJFactory.getOpCard(KaiJinJin1AddJin2_8ZhangImpl.class).checkOpCard(this.set.getMJSetPos(this.set.getDPos()), 0);
                break;
            case BENSHEN_JIN1_ADD_JIN2_11ZHANG://翻开一张牌，该牌的顺一位和二位做为混牌  11张；
                MJFactory.getOpCard(KaiJinBenShenAddJin1AddJin2_11ZhangImpl.class).checkOpCard(this.set.getMJSetPos(this.set.getDPos()), 0);
                break;
            case NOT:
                return false;
            default:
                return false;
        }
        return true;
    }

    /**
     * 换三张
     *
     * @return
     */
    public boolean atFirstHuChangeCard() {
        if (MJTemplateRoomEnum.ChangeCardType.NOT.equals(getRoom().wanFa_ChangeCardType())) {
            return false;
        }
        //初始化换三张
        this.getSet().initSanZhang();
        return checkOpTypes(OpType.HuanSanZhang, true);

    }

    /**
     * 起手报听｜天听
     *
     * @return
     */
    public boolean atFirstHuTianTing() {
        if (!getRoom().isWanFa_TianTing()) {
            return false;
        }
        return atFirstHuCheckOpTpye(OpType.TianTing, true, false);
    }

    /**
     * 定缺
     *
     * @return
     */
    public boolean atFirstHuDingQue() {
        if (!getRoom().isWanFa_DingQue()) {
            return false;
        }
        return checkOpTypes(new ArrayList<>(Arrays.asList(OpType.Wan, OpType.Tiao, OpType.Tong)), true);
    }

    /**
     * 公共处理方法
     *
     * @param opType     操作类型
     * @param addPass    是否需要pass
     * @param publicWait 是否公开
     * @return
     */
    public boolean atFirstHuCheckOpTpye(OpType opType, boolean addPass, boolean publicWait) {
        this.set.getSetPosMgr().checkOpType(this.set.getDPos(), 0, opType);// 补花以后再检测天湖
        NextOpType nOpType = this.set.getSetPosMgr().exeCardAction(opType);
        if (null != nOpType) {
            for (int posID : nOpType.getPosOpTypeListMap().keySet()) {
                AbsMJRoundPos nextPos = this.nextRoundPos(posID);
                if (nOpType.getPosOpTypeListMap().containsKey(posID)) {
                    nextPos.addOpType(nOpType.getPosOpTypeListMap().get(posID));
                }
                if (addPass) {
                    nextPos.addOpType(OpType.Pass);
                }
                nextPos.setPublicWait(publicWait);
                setStartTime(CommTime.nowSecond());
                this.roundPosDict.put(nextPos.getOpPos(), nextPos);
            }
            return true;
        }
        return false;
    }

    /**
     * 共处理方法
     *
     * @param opType
     * @param publicWait
     * @return
     */
    public boolean checkOpTypes(OpType opType, boolean publicWait) {
        return checkOpTypes(new ArrayList<>(Arrays.asList(opType)), publicWait);
    }

    public boolean checkOpTypes(List<OpType> opTypes, boolean publicWait) {
        this.set.getPosDict().values().forEach(k -> {
            AbsMJRoundPos tmPos = this.nextRoundPos(k.getPosID());
            opTypes.stream().forEach(op -> {
                if (tmPos.getPos().checkOpType(0, op)) {
                    tmPos.addOpType(op);
                }
            });
            if (tmPos.getRecieveOpTypes().isEmpty()) {
                return;
            }
            //客户端要显示XX状态中，需要公开所有玩家
            tmPos.setPublicWait(publicWait);
            this.roundPosDict.put(tmPos.getOpPos(), tmPos);
        });
        return !roundPosDict.isEmpty();
    }

    /**
     * 开局补花
     *
     * @return
     */
    public boolean atFirstHuBuHua() {
        getSet().getSetPosMgr().startSetApplique();
        return !getRoom().getBuHuaTypeSet().isEmpty();
    }

    @Override
    protected boolean autoOutCard(int sec) {

        if (sec - this.startTime < 1) {
            return false;
        }
        MJTemplateRoundPos roundPos;
        MJTemplateSetPos sPos;
        MJTemplateRoomSet roomSet;
        int cardID;
        for (int posID = 0; posID < this.room.getPlayerNum(); posID++) {
            roundPos = (MJTemplateRoundPos) this.roundPosDict.get(posID);
            if (null == roundPos) {
                continue;
            }
            sPos = roundPos.getPos();
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
            if (getStartTime() + getRoom().wanFa_DingQue_Time() <= CommTime.nowSecond() && opList.stream().allMatch(opType -> opType.equals(OpType.Wan) || opType.equals(OpType.Tiao) || opType.equals(OpType.Tong))) {
                // 没有相应的动作直接过
                this.opCard(new WebSocketRequestDelegate(), sPos.getPosID(), opList.get(RandomUtils.nextInt(0, opList.size())), MJOpCard.OpCard(0));
                continue;
            }
            if (!sPos.isTing()) {
                continue;
            }
            if (opList.contains(OpType.Gang) || opList.contains(OpType.JieGang) || opList.contains(OpType.AnGang) || opList.contains(OpType.Hu)) {
                continue;
            }
            if (opList.contains(OpType.Out)) {
                if (null != sPos.getHandCard()) {
                    cardID = sPos.getHandCard().cardID;
                } else {
                    continue;
                }
                this.opCard(new WebSocketRequestDelegate(), roundPos.getOpPos(), OpType.Out, MJOpCard.OpCard(cardID), true);
            }
        }

        return false;
    }

    @Override
    protected AbsMJRoundPos nextRoundPos(int pos) {
        return new MJTemplateRoundPos(this, pos);
    }

    @Override
    protected boolean tryStartRoundOther(AbsMJSetRound preRound) {
        // 上一轮接牌， 本轮继续出牌
        if (preRound.getOpType() == OpType.BaoTing) {
            return tryStartRoundOut(preRound);
        }
        if (preRound.getOpType() == OpType.TianTing) {
            if (startWithGetCard(this.set.getDPos(), true)) {
                notifyStart();
                return true;
            }
        }
        // 上一轮换三张
        if (preRound.getOpType() == OpType.HuanSanZhang) {
            return tryStartRoundChangeCard(preRound);
        }
        // 万，条，筒
        if (OpType.Wan.equals(preRound.getOpType()) || OpType.Tiao.equals(preRound.getOpType())
                || OpType.Tong.equals(preRound.getOpType())) {
            return tryStartRoundDingQue();
        }

        // 自摸，炮胡
        if (OpType.Hu.equals(preRound.getOpType()) || OpType.JiePao.equals(preRound.getOpType())
                || OpType.QiangGangHu.equals(preRound.getOpType())) {
            return tryStartRoundHu(preRound);
        }
        if (preRound.getOpType() == OpType.YaoGang) {
            return tryStartRoundYaoGang(preRound);
        }
        if (preRound.getOpType() == OpType.JingDiao) {
            return tryStartRoundJingDiao(preRound);
        }
        return false;
    }

    /**
     * @param preRound
     * @return
     */
    protected boolean tryStartRoundJingDiao(AbsMJSetRound preRound) {
        return tryStartRoundOut(preRound);
    }

    /**
     * 摇杠
     *
     * @param preRound
     * @return
     */
    protected boolean tryStartRoundYaoGang(AbsMJSetRound preRound) {
        int exeOpPos = getPreRound().getExeOpPos();
        AbsMJSetPos mjSetPos = this.set.getMJSetPos(exeOpPos);
        Integer opValue = mjSetPos.getPublicCardList().get(mjSetPos.sizePublicCardList() - 1).get(0);
        if (opValue == OpType.AnGang.value()) {
            return tryStartRoundAnGang(preRound);
        } else {
            return tryStartRoundGang(preRound);
        }
    }

    /**
     * 胡牌处理 正常模式胡牌就结束 无需处理
     *
     * @param preRound
     * @return
     */
    protected boolean tryStartRoundHu(AbsMJSetRound preRound) {
        return false;
    }


    /**
     * 如果没有三张  起手13张报听
     * 如果起手13张报听 庄稼开始摸打
     *
     * @return
     */
    protected boolean tryStartRoundDingQue() {
        getSet().sendSetPosQingQue();
        //再起手报听
        if (atFirstHuTianTing()) {
            notifyStart();
            return true;
        }
        // 只能顺序的抓牌，打牌
        if (!startWithGetCard(set.getDPos(), true)) {
            return false;
        }
        notifyStart();
        return true;
    }

    /**
     * 海底捞月
     *
     * @param pos
     * @param isNormalMo
     * @return
     */
    public boolean startWithHaiDiLaoYue(int pos, boolean isNormalMo) {
        if (MJTemplateRoomEnum.HaiDiLaoYue.EACH_DEAL_CARD.equals(getRoom().wanFa_HaiDiLaoYue())) {
            return startWithHDLY_EachDealCard(pos, isNormalMo);
        } else if (MJTemplateRoomEnum.HaiDiLaoYue.DEAL_ONE_CARD.equals(getRoom().wanFa_HaiDiLaoYue())) {
            return startWithHDLY_DealOneCard(pos, isNormalMo);
        }
        return false;
    }

    /**
     * 海底捞月 每个玩家发张牌检测胡
     *
     * @param pos
     * @param isNormalMo
     * @return
     */
    public boolean startWithHDLY_EachDealCard(int pos, boolean isNormalMo) {
        if (!getSet().getSetCard().isStartHaiDiLaoYue(getSet().getPlayerNum())) {
            return false;
        }
        this.isHdly = true;
        for (int i = 0; i < room.getPlayerNum(); i++) {
            int nextOpPos = (pos + i) % this.room.getPlayerNum();
            AbsMJSetPos mSetPOs = this.set.getMJSetPos(nextOpPos);
            if (null == mSetPOs.getHandCard()) {
                if (null == this.set.getCard(nextOpPos, isNormalMo)) {
                    continue;
                }
            }
            AbsMJRoundPos tmPos = this.nextRoundPos(nextOpPos);
            if (tmPos.getPos().checkOpType(0, OpType.Hu)) {
                tmPos.addOpType(OpType.Hu);
            }
        }
        //如果没有人能胡，则结束游戏；
        if (roundPosDict.isEmpty()) {
            return false;
        }
        notifyStart();
        return true;

    }

    /**
     * 海底捞月 只发一张 每个玩家检测胡
     *
     * @param pos
     * @param isNormalMo
     * @return
     */
    public boolean startWithHDLY_DealOneCard(int pos, boolean isNormalMo) {
        if (!getSet().getSetCard().isStartHaiDiLaoYue(1)) {
            return false;
        }
        this.isHdly = true;
        AbsMJSetPos mSetPOs = this.set.getMJSetPos(pos);
        //先摸出一张牌
        MJCard card = this.set.getSetCard().pop(isNormalMo, this.set.getGodInfo().godHandCard(mSetPOs));
        if (card == null) {
            return false;
        }
        for (int i = 0; i < room.getPlayerNum(); i++) {
            int nextOpPos = (pos + i) % this.room.getPlayerNum();
            mSetPOs = this.set.getMJSetPos(nextOpPos);
            mSetPOs.setHandCard(card);
            checkOpTypes(OpType.Hu, false);
        }
        //如果没有人能胡，则结束游戏；
        if (roundPosDict.isEmpty()) {
            return false;
        }
        notifyStart();
        return true;
    }


    /**
     * 换三张完后，需要让每个玩家选择万筒条缺哪一门胡牌；
     */
    protected boolean tryStartRoundChangeCard(AbsMJSetRound preRound) {
        ((MJTemplateRoomSet) this.set).changeSanZhangCards();
        if (atFirstHuDingQue()) {
        } else if (atFirstHuTianTing()) {
        } else if (!startWithGetCard(this.set.getDPos(), true)) {
            return false;
        }
        notifyStart();
        return true;
    }

    @Override
    protected boolean checkExistClearPass() {
        return false;
    }

    /**
     * 通知回合开始
     */
    public void notifyStart() {
        BaseSendMsg other = this.startRound(this.set.getRoom().getRoomID(), this.getNotify_RoundInfo(-1));
        for (int posID = 0; posID < this.room.getPlayerNum(); posID++) {
            if (this.roundPosDict.containsKey(posID)) {
                this.set.getRoomPlayBack().playBack2Pos(posID,
                        this.startRound(this.set.getRoom().getRoomID(), this.getNotify_RoundInfo(posID)),
                        set.getSetPosMgr().getAllPlayBackNotify());
            } else {
                this.set.getRoom().getRoomPosMgr().notify2PosClearLatelyOutCardTime(posID, other);
            }
        }
        //新一轮重置数据
        this.set.getLastOpInfo().setLastShotTime(CommTime.nowSecond());
        this.roundPosDict.values().stream().forEach(roundPos -> ((MJTemplateRoundPos) roundPos).reSetRoomPosOp());
    }

    /**
     * 获取本轮信息
     *
     * @param pos 位置
     * @return
     */

    public BaseMJRoom_SetRound getNotify_RoundInfo(int pos) {
        ret = newMJRoomSetRound();
        ret.setWaitID(this.roundID);
        ret.setStartWaitSec(this.startTime);
        ret.setRunWaitSec(CommTime.nowSecond() - this.startTime);
        this.roundPosDict.values().stream().forEach(roundPos -> {
            if (roundPos.getOpType() != null) {
                return;
            }
            // 自己 或 公开
            if (pos == roundPos.getOpPos() || roundPos.isPublicWait()) {
                boolean isSelf = pos == roundPos.getOpPos();
                BaseMJRoom_RoundPos data = ((MJTemplateRoundPos) roundPos).roomRoundPosInfo(isSelf, isBuChiFuDaFu());
                ret.addOpPosList(data);
                ((MJTemplateRoundPos) roundPos).reSetRoomPosOp();
            }
        });
        ((MJTemplateRoom_SetRound) ret).setHdly(isHdly);
        return ret;
    }

    /**
     * 是否可以吃幅打幅
     *
     * @return T:不能吃幅打幅，F:可以
     */
    @Override
    protected boolean isBuChiFuDaFu() {
        return true;
    }

    protected BaseMJRoom_SetRound newMJRoomSetRound() {
        return new MJTemplateRoom_SetRound();
    }

    @Override
    public MJTemplateRoom getRoom() {
        return (MJTemplateRoom) super.getRoom();
    }

    /**
     * 下位置操作类型
     *
     * @param nextPos
     * @return
     */
    @Override
    public AbsMJRoundPos nextPosOpType(AbsMJRoundPos nextPos) {
        MJTemplateSetPos pos = (MJTemplateSetPos) nextPos.getPos();
        //重置不能出的牌
        pos.addBuNengChuList();
        if (pos.checkOpType(0, OpType.TingYouJin)) {
            nextPos.addOpType(OpType.TingYouJin);
        } else {
            if (pos.checkOpType(0, OpType.Ting)) {
                nextPos.addOpType(OpType.Ting);
            }
        }
        if (!pos.isYaoGang) {
            if (pos.checkOpType(0, OpType.AnGang)) {
                nextPos.addOpType(OpType.AnGang);
            }
            if (pos.checkOpType(0, OpType.Gang)) {
                nextPos.addOpType(OpType.Gang);
            }
            if (MJTemplateRoomEnum.YaoGang.YAO_GANG.equals(getRoom().wanFa_YaoGang())) {
                pos.addYaoGangList(pos.getAnGangList());
                pos.addYaoGangList(pos.getBuGangList());
                if (CollectionUtils.isNotEmpty(pos.yaoGangList)) {
                    nextPos.addOpType(OpType.YaoGang);
                }
            }
        }
        nextPos.addOpType(OpType.Out);
        return nextPos;
    }

    /**
     * 检查是否直接过
     *
     * @return
     */
    @Override
    protected boolean checkPass() {
        for (int i = 0; i < this.room.getPlayerNum(); i++) {
            if (set.getMJSetPos(i).getHandCard() != null) {
                AbsMJRoundPos nextPos = this.nextRoundPos(i);
                nextPos.addOpType(OpType.Out);
                this.roundPosDict.put(nextPos.getOpPos(), nextPos);
                notifyStart();
                return true;
            }
        }
        return false;
    }

    /**
     * 上回合是过牌后的操作
     */
    @Override
    public boolean passOther() {
        //起手听牌
        if (getPreRound().getRoundPosDict().values().stream().anyMatch(k -> k.checkRecieveOpTypes(OpType.TianTing))) {
            if (!startWithGetCard(getSet().getDPos(), true)) {
                return false;
            }
            notifyStart();
            return true;
        }
        return false;
    }

    @Override
    public MJTemplateRoomSet getSet() {
        return (MJTemplateRoomSet) super.getSet();
    }

}
