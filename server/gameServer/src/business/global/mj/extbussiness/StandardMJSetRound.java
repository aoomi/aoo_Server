package business.global.mj.extbussiness;

import business.global.mj.*;
import business.global.mj.extbussiness.dto.StandardMJOpCard;
import business.global.mj.extbussiness.dto.StandardMJRoom_RoundPos;
import business.global.mj.extbussiness.optype.StandardMJUtil;
import business.global.mj.set.MJOpCard;
import cenum.mj.MJOpCardError;
import cenum.mj.OpType;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import com.ddm.server.websocket.handler.requset.WebSocketRequestDelegate;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.mj.BaseMJRoom_SetRound;
import jsproto.c2s.cclass.mj.BaseMJSet_Pos;
import jsproto.c2s.iclass.mj._PosOpCard;
import jsproto.c2s.iclass.mj._StartRound;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 麻将 回合逻辑 每一次等待操作，都是一个round
 *
 * @author Huaxing
 */

public class StandardMJSetRound extends AbsMJSetRound {

    public StandardMJSetRound(AbsMJSetRoom set, int roundID) {
        super(set, roundID);
    }

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
     * 位置操作牌
     *
     * @param opPosRet 操作位置
     * @param isFlash  是否动画
     */
    protected void posOpCardRet(int opPosRet, boolean isFlash) {
        int opCardID = this.set.getLastOpInfo().getLastOutCard();
        StandardMJSetPos sPos = (StandardMJSetPos) this.set.getMJSetPos(opPosRet);
        sPos.getPosOpNotice().clearTingCardMap();
        // 刷新可胡列表
        this.refreshHuCardTypes(sPos);
        // 吃碰杠-清理牌
        if (OpType.Peng.equals(this.getOpType()) || OpType.JieGang.equals(this.getOpType())
                || OpType.Chi.equals(this.getOpType())) {
            // 主要是跟打清空使用。 清空打牌信息
            this.cleanOutCardInfo();
            this.set.getLastOpInfo().clearLastOutCard();
            if (this.checkExistClearPass()) {
                // 过手
                sPos.clearPass();
            }
            this.set.getSetPosMgr().clearOpTypeInfoList();
        }
        // 补杠、暗杠时候，操作牌ID == 0
        if (OpType.Gang.equals(this.getOpType()) || OpType.AnGang.equals(this.getOpType())) {
            this.cleanOutCardInfo();
            opCardID = 0;
        }
        this.setExeOpPos(opPosRet);
        BaseMJSet_Pos posInfoOther = sPos.getNotify(false);
        BaseMJSet_Pos posInfoSelf = sPos.getNotify(true);
        this.set.getRoomPlayBack().playBack2Pos(opPosRet, this.posOpCard(this.room.getRoomID(), opPosRet, posInfoSelf, this.getOpType(), opCardID, isFlash), set.getSetPosMgr().getAllPlayBackNotify());
        this.set.getRoom().getRoomPosMgr().notify2ExcludePosID(opPosRet, this.posOpCard(this.room.getRoomID(), opPosRet, posInfoOther, this.getOpType(), opCardID, isFlash));
    }

    /**
     * 无用的方法
     *
     * @return T:不能吃幅打幅，F:可以
     */
    protected boolean isBuChiFuDaFu() {
        return true;
    }

    /**
     * 吃碰是否需要清除漏胡
     */
    @Override
    protected boolean checkExistClearPass() {
        return ((StandardMJRoom) room).isCheckExistClearPass();
    }

    @Override
    public int opCard(WebSocketRequest request, int opPos, OpType opType, MJOpCard mOpCard) {
        return opCard(request, opPos, opType, mOpCard, false);
    }

    /**
     * 位置操作牌
     */
    @Override
    public <T> BaseSendMsg posOpCard(long roomID, int pos, T set_Pos, OpType opType, int opCard, boolean isFlash) {
        return _PosOpCard.make(roomID, pos, set_Pos, opType, opCard, isFlash, getRoom().getGameName());
    }


    @Override
    protected <T> BaseSendMsg startRound(long roomID, T room_SetWait) {
        return _StartRound.make(roomID, room_SetWait, getRoom().getGameName());
    }

    /**
     * 主要是跟打清空使用。 清空打牌信息
     */
    protected void cleanOutCardInfo() {
        ((StandardMJRoomSet) set).setChaoState(2);
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

    @Override
    protected boolean autoOutCard(int sec) {
        if (sec - this.startTime < 1) {
            return false;
        }
        StandardMJRoundPos roundPos;
        StandardMJSetPos sPos;
        for (int posID = 0; posID < this.room.getPlayerNum(); posID++) {
            roundPos = (StandardMJRoundPos) this.roundPosDict.get(posID);
            if (null == roundPos) {
                continue;
            }
            sPos = (StandardMJSetPos) roundPos.getPos();
            if (null == sPos || sPos.getRoomPos().isTrusteeship()) {
                continue;
            }
            List<OpType> opList = roundPos.getRecieveOpTypes();
            if (CollectionUtils.isEmpty(opList)) {
                continue;
            }
            //换三张
            if (this.startTime + 30 <= CommTime.nowSecond() && opList.contains(OpType.HuanSanZhang) && roundPos.getOpType() == null) {
                this.opCard(new WebSocketRequestDelegate(), roundPos.getOpPos(), OpType.HuanSanZhang, StandardMJOpCard.OpCard(0, sPos.getFirstChangeCardList()));
                continue;
            }
            if (sPos.isTing()) {
                int cardID = 0;
                if (opList.contains(OpType.Gang) || opList.contains(OpType.Hu) || opList.contains(OpType.JiePao) || opList.contains(OpType.JieGang) || opList.contains(OpType.AnGang)) {
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
        }
        return false;
    }

    /**
     * 开始本回合,并摸牌
     *
     * @param pos        位置
     * @param isNormalMo 正摸
     * @return 摸牌结果
     */
    @Override
    public boolean startWithGetCard(int pos, boolean isNormalMo) {
        // 抓牌
        if (set.isAtFirstHu()) {
            set.setAtFirstHu(false);
            // 作弊情况下，已经有手牌
            if (null == this.set.getMJSetPos(pos).getHandCard()) {
                if (null == this.set.getCard(pos, isNormalMo)) {
                    return false;
                }
            }
            //换张/换牌（模板麻将过来）
            if (atFirstHuChangeCard()) {
                return true;
            }
        }
        // 作弊情况下，已经有手牌
        if (null == this.set.getMJSetPos(pos).getHandCard()) {
            if (null == this.set.getCard(pos, isNormalMo)) {
                return false;
            }
        }
        boolean result = MJRoundPos(pos);
        ((StandardMJRoomSet) this.set).setTiAnHuFlag(false);
        return result;
    }

    //--------模板麻将抄袭部分

    /**
     * 换三张
     *
     * @return
     */
    public boolean atFirstHuChangeCard() {
        if (((StandardMJRoom) room).getHuanZhangNum() <= 0) {
            return false;
        }
        //初始化换三张
        StandardMJUtil.get().initSanZhang(((StandardMJRoomSet) getSet()));
        return checkOpTypes(OpType.HuanSanZhang, true);
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
    //--------模板麻将抄袭部分end


    /**
     * 下位置操作类型
     *
     * @param nextPos
     * @return
     */
    public AbsMJRoundPos nextPosOpType(AbsMJRoundPos nextPos) {
        if (nextPos.getPos().checkOpType(0, OpType.Ting)) {
            nextPos.addOpType(OpType.Ting);
            if (((StandardMJRoom) getRoom()).isBaoTing() && !((StandardMJSetPos) nextPos.getPos()).isTing()) {
                nextPos.addOpType(OpType.BaoTing);
            }
        }

        if (nextPos.getPos().checkOpType(0, OpType.AnGang)) {
            nextPos.addOpType(OpType.AnGang);
        }
        if (nextPos.getPos().checkOpType(0, OpType.Gang)) {
            nextPos.addOpType(OpType.Gang);
        }
        nextPos.addOpType(OpType.Out);
        return nextPos;
    }

    @Override
    protected boolean tryStartRoundOther(AbsMJSetRound preRound) {
        if (OpType.BaoTing.equals(preRound.getOpType())) {
            //庄家出过牌，接下来报听全是要打牌，会轮到下家出牌
            return tryStartRoundOut(preRound);
        }
        // 上一轮换三张
        if (preRound.getOpType() == OpType.HuanSanZhang) {
            return tryStartRoundChangeCard(preRound);
        }
        return false;
    }

    /**
     * 换三张完后，需要让每个玩家选择万筒条缺哪一门胡牌；
     */
    protected boolean tryStartRoundChangeCard(AbsMJSetRound preRound) {
        changeSanZhangCards();
        if (!startWithGetCard(this.set.getDPos(), true)) {
            return false;
        }
        notifyStart();
        return true;
    }

    /**
     * 换牌
     */
    public void changeSanZhangCards() {
        StandardMJRoom room = (StandardMJRoom) getRoom();
        StandardMJRoomEnum.HuanZhangOderBy changeCardOderBy = room.huanZhangOderBy();
        if (StandardMJRoomEnum.HuanZhangOderBy.SuiJi.equals(changeCardOderBy)) {
            //随机出与庄家互换对位置
            int changDPos = -1;
            while (changDPos == -1 || changDPos == set.getDPos()) {
                changDPos = RandomUtils.nextInt(0, set.getPlayerNum());
            }
            StandardMJSetPos dSetPos;
            StandardMJSetPos secondSetPos;
            if (set.getPlayerNum() == 3) {
                int secondPos = changDPos;
                StandardMJSetPos thirdSetPos = (StandardMJSetPos) set.getPosDict().values().stream().filter(k -> k.getPosID() != set.getDPos() && k.getPosID() != secondPos).findFirst().get();
                dSetPos = (StandardMJSetPos) set.getMJSetPos(set.getDPos());
                secondSetPos = (StandardMJSetPos) set.getMJSetPos(secondPos);
                dSetPos.getChangeCardList().forEach(k -> secondSetPos.addPrivateCard(set.getSetCard().getCardByID(k)));
                secondSetPos.getChangeCardList().forEach(k -> thirdSetPos.addPrivateCard(set.getSetCard().getCardByID(k)));
                thirdSetPos.getChangeCardList().forEach(k -> dSetPos.addPrivateCard(set.getSetCard().getCardByID(k)));
            } else {
                int secondPos = changDPos;
                dSetPos = (StandardMJSetPos) set.getMJSetPos(set.getDPos());
                secondSetPos = (StandardMJSetPos) set.getMJSetPos(secondPos);
                dSetPos.getChangeCardList().forEach(k -> secondSetPos.addPrivateCard(set.getSetCard().getCardByID(k)));
                secondSetPos.getChangeCardList().forEach(k -> dSetPos.addPrivateCard(set.getSetCard().getCardByID(k)));
                if (set.getPlayerNum() == 4) {
                    List<AbsMJSetPos> setPosList = set.getPosDict().values().stream().filter(k -> k.getPosID() != set.getDPos() && k.getPosID() != secondPos).collect(Collectors.toList());
                    ((StandardMJSetPos) setPosList.get(0)).getChangeCardList().forEach(k -> setPosList.get(1).addPrivateCard(set.getSetCard().getCardByID(k)));
                    ((StandardMJSetPos) setPosList.get(1)).getChangeCardList().forEach(k -> setPosList.get(0).addPrivateCard(set.getSetCard().getCardByID(k)));
                }
            }
            if (Objects.nonNull(dSetPos) && Objects.isNull(dSetPos.getHandCard())) {
                MJCard remove = dSetPos.getPrivateCard().remove(dSetPos.sizePrivateCard() - 1);
                dSetPos.setHandCard(remove);
            }
        } else {
            StandardMJSetPos setPos;
            int changOrder = 1;
            if (StandardMJRoomEnum.HuanZhangOderBy.NiShiZhen.equals(changeCardOderBy)) {
                changOrder = -1;
                //只有四个人才有对家换
            } else if (StandardMJRoomEnum.HuanZhangOderBy.DuiJia.equals(changeCardOderBy) && set.getPlayerNum() == 4) {
                changOrder = 2;
            }
            StandardMJSetCard setCard = (StandardMJSetCard) set.getSetCard();
            for (AbsMJSetPos abSetPos : set.getPosDict().values()) {
                setPos = (StandardMJSetPos) abSetPos;
                StandardMJSetPos setPos1 = (StandardMJSetPos) set.getMJSetPos((abSetPos.getPosID() - changOrder + set.getPlayerNum()) % set.getPlayerNum());
                setPos.getChangeCardList().forEach(k -> setPos1.addPrivateCard(setCard.getCardByID(k)));
                setPos1.sortCards();
                //如果是庄家 并且手牌被拿去换了 需要补张回去
                if (setPos1.getPosID() == set.getDPos() && Objects.isNull(setPos1.getHandCard())) {
                    MJCard remove = setPos1.getPrivateCard().remove(setPos1.sizePrivateCard() - 1);
                    setPos1.setHandCard(remove);
                }
                setPos1.getFirstChangeCardList().clear();
                setPos1.getFirstChangeCardList().addAll(setPos.getChangeCardList());
                setPos.getChangeCardList().clear();
            }
        }
        set.getPosDict().values().forEach(k -> {
            if (Objects.isNull(k.getHandCard())) {
                k.calcHuFan();
            }
        });
        set.sendSetPosCard();
    }

    /**
     * 过
     *
     * @param preRound
     * @return
     */
    protected boolean tryStartRoundPass(AbsMJSetRound preRound) {
        if (preRound.getWaitDealRound() != null) {
            preRound = preRound.getWaitDealRound();
        }
        // 上次的出牌，需要继续处理
        if (preRound.checkExistNextRoundOp()) {
            // 检查下回合操作位置
            return this.checkNextRoundOpPos(preRound);
        } else {
            //其他操作
            if (passOther()) {
                return true;
            }
            // 检查是否直接过
            if (this.checkPass()) {
                return true;
            } else if (preRound.getOpType() == OpType.Out || preRound.getOpType() == OpType.BaoTing) {
                // 无法再处理了，下家抓牌
                // 获取用户位置ID
                return checkQtherPing();
            } else if (preRound.getOpType() == OpType.Gang) {
                return checkQtherQiang();
            }
            return true;
        }
    }

    /**
     * 获取本轮信息
     *
     * @param pos 位置
     * @return
     */
    public BaseMJRoom_SetRound getNotify_RoundInfo(int pos) {
        ret = new BaseMJRoom_SetRound();
        ret.setWaitID(this.roundID);
        ret.setStartWaitSec(this.startTime);
        ret.setRunWaitSec(CommTime.nowSecond() - this.startTime);
        for (AbsMJRoundPos roundPos : this.roundPosDict.values()) {
            if (roundPos.getOpType() != null) {
                continue;
            }
            // 自己 或 公开
            if (pos == roundPos.getOpPos() || roundPos.isPublicWait()) {
                StandardMJRoom_RoundPos data = new StandardMJRoom_RoundPos();
                boolean isSelf = pos == roundPos.getOpPos();
                if (isSelf) {
                    //如果是自己
                    data.setOpList(roundPos.getRecieveOpTypes());
                } else if (roundPos.isPublicWait() && roundPos.checkRecieveOpTypes(OpType.Out)) {
                    //如果是公开 并且存在出牌  只通知出牌
                    data.setOpList(roundPos.getRecieveOpTypes().stream().filter(OpType.Out::equals).collect(Collectors.toList()));
                } else if (roundPos.isPublicWait()) {
                    //如果是公开
                    data.setOpList(roundPos.getRecieveOpTypes());
                }
                //换三张
                List<Integer> firstChangeCardList = ((StandardMJSetPos) roundPos.getPos()).getFirstChangeCardList();
                if (CollectionUtils.isNotEmpty(firstChangeCardList)) {
                    if (isSelf) {
                        data.setFirstChangeCardList(firstChangeCardList);
                    } else {
                        data.setFirstChangeCardList(Collections.nCopies(firstChangeCardList.size(), 0));
                    }
                }
                data.setChiList(roundPos.getPos().getPosOpNotice().getChiList());
                data.setLastOpCard(roundPos.getLastOutCard());
                data.setWaitOpPos(roundPos.getOpPos());
                data.setTingCardMap(isSelf ? roundPos.getPos().getPosOpNotice().getTingCardMap() : null);
                StandardMJSetPos setPos = (StandardMJSetPos) roundPos.getPos();
                data.setTingInfoList(isSelf ? new ArrayList(setPos.getTingInfoListMap().values()) : null);
                if (CollectionUtils.isNotEmpty(data.getOpList())) {
                    if (data.getOpList().contains(OpType.AnGang)) {
                        data.setAnGangList(setPos.getAnGangList());
                    }
                    if (data.getOpList().contains(OpType.JieGang)) {
                        data.setJieGangList(setPos.getJieGangList());
                    }
                    if (data.getOpList().contains(OpType.Gang)) {
                        data.setBuGangList(setPos.getBuGangList());
                    }
                }
                data.setBuChuList(isSelf ? setPos.getBuNengChuList() : null);
                ret.addOpPosList(data);
                // 设置动作列表
                roundPos.getPos().getPosOpRecord().setOpList(data.getOpList());
                if (room.isConnectClearTrusteeship()) {
                    // 重新记录打牌时间
                    roundPos.getPos().getRoomPos().setLatelyOutCardTime(CommTime.nowMS());
                }
                // 设置最后操作时间
                this.set.getLastOpInfo().setLastShotTime(CommTime.nowSecond());

            }
        }
        return ret;
    }

    //---------------下面方法根据需求自己扩展----------------------

    /**
     * 每回合机器人类适配
     * 根据自己游戏，看需要不需要扩展该类，并重写这个方法
     *
     * @return
     */
    @Override
    public void RobothandCrad(int posID) {
        if (this.getEndTime() > 0) {
            return;
        }
        if (this.getRoundPosDict().containsKey(posID)) {
            new StandardMJRobotOpCard(this).RobothandCrad(posID);
        }
    }

    /**
     * 每回合操作位置适配
     * 根据自己游戏，看需要不需要扩展该类，并重写这个方法
     *
     * @return
     */
    @Override
    protected AbsMJRoundPos nextRoundPos(int pos) {
        return new StandardMJRoundPos(this, pos);
    }

}
