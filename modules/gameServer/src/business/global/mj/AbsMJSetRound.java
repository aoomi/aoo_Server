package business.global.mj;

import business.global.mj.robot.MJRobotOpCard;
import business.global.mj.set.MJOpCard;
import business.global.room.mj.MahjongRoom;
import cenum.mj.HuType;
import cenum.mj.MJCEnum;
import cenum.mj.OpType;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.mj.BaseMJRoom_RoundPos;
import jsproto.c2s.cclass.mj.BaseMJRoom_SetRound;
import jsproto.c2s.cclass.mj.BaseMJSet_Pos;
import jsproto.c2s.cclass.mj.NextRoundOpPos;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Data
public abstract class AbsMJSetRound {
    /**
     * 当局信息
     */
    protected AbsMJSetRoom set;
    /**
     * 房间信息
     */
    protected MahjongRoom room;
    /**
     * 第几回合
     */
    protected int roundID;
    /**
     * 回合开始时间
     */
    protected int startTime;
    /**
     * 回合开始时间毫秒级
     */
    protected long startTimeMillis;
    /**
     * 回合结束时间
     */
    protected int endTime;
    /**
     * 回合结束时间毫秒级
     */
    protected long endTimeMillis;
    /**
     * 指定玩家的回合操作
     */
    protected Map<Integer, AbsMJRoundPos> roundPosDict = new ConcurrentHashMap<>(4);
    /**
     * 是否结束本回合
     */
    protected boolean isSetHuEnd = false;
    /**
     * 是否操作出牌
     */
    protected OpType opType = null;
    /**
     * 操作的牌
     */
    protected int opCard = 0;
    /**
     * 获取下回合的操作位置
     */
    protected List<NextRoundOpPos<AbsMJSetRound>> nextRoundOp = Collections.synchronizedList(new ArrayList<>(1));
    /**
     * 等待接受处理的round
     */
    protected AbsMJSetRound waitDealRound = null;
    /**
     * 本回合操作位置
     */
    protected int exeOpPos = -1;
    /**
     * 回合信息记录
     */
    protected BaseMJRoom_SetRound ret;
    /**
     * 用来查看是否线程更新
     */
    private int updateTime = 0;

    public AbsMJSetRound(AbsMJSetRoom set, int roundID) {
        this.roundID = roundID;
        this.set = set;
        this.room = set.getRoom();
        this.startTime = CommTime.nowSecond();
        this.startTimeMillis = CommTime.nowMS();
    }

    /**
     * 清空回合信息
     */
    public void roundClear() {
        if (null != roundPosDict) {
            this.roundPosDict.forEach((key, value) -> {
                if (null != value) {
                    value.clear();
                }
            });
            this.roundPosDict.clear();
            this.roundPosDict = null;
        }
        if (null != nextRoundOp) {
            this.nextRoundOp.clear();
            this.nextRoundOp = null;
        }
        this.waitDealRound = null;
        this.set = null;
        this.room = null;
        this.ret = null;
    }

    /**
     * 操作牌
     *
     * @param opPos   操作位置
     * @param opType  动作类型
     * @param mOpCard 操作牌
     */
    public abstract int opCard(WebSocketRequest request, int opPos, OpType opType, MJOpCard mOpCard);

    /**
     * 检查是否有操作者
     *
     * @param opPos
     * @return
     */
    public boolean isRoundPosDict(int opPos) {
        // 检查动作
        if (MapUtils.isEmpty(this.getRoundPosDict())) {
            return false;
        }
        // 检查是否该位置玩家操作
        return null != this.roundPosDict.get(opPos);
    }

    /**
     * 添加下回合操作列表
     *
     * @param next
     */
    public void addNextRoundOpList(NextRoundOpPos<AbsMJSetRound> next) {
        this.getNextRoundOp().add(next);
    }

    /**
     * 检查是否存在下回合操作
     *
     * @return
     */
    public boolean checkExistNextRoundOp() {
        return CollectionUtils.isNotEmpty(this.getNextRoundOp());
    }

    /**
     * 移除回合操作
     *
     * @return
     */
    public NextRoundOpPos<AbsMJSetRound> removeNextRountOp() {
        return this.getNextRoundOp().remove(0);
    }

    /**
     * 清空
     */
    public void clear() {
        this.roundID = 1;
        this.roundPosDict.clear();
        this.nextRoundOp = null;
        this.waitDealRound = null;
    }


    /**
     * 设置操作的牌
     *
     * @param opCard
     */
    public void setOpCard(int opCard) {
        if (this.opCard == opCard) {
            return;
        }
        this.opCard = opCard;
    }

    /**
     * 获得前一轮
     *
     * @return
     */
    public AbsMJSetRound getPreRound() {
        return this.getSet().getPreRound();
    }

    /**
     * 尝试结束当前回合
     *
     * @param isHu
     */
    public boolean tryEndRound(boolean isHu) {
        // 遍历检查是否每个人都执行完毕了
        boolean allOp = this.roundPosDict.values().stream().allMatch(k -> Objects.nonNull(k.getOpType()));
        if (allOp || isHu) {
            this.endTime = CommTime.nowSecond();
            this.endTimeMillis = CommTime.nowMS();
            return true;
        }
        return false;
    }

    /**
     * 更新当前回合操作。
     *
     * @param sec
     * @return
     */
    public boolean update(int sec) {
        // 记录更新时间
        this.setUpdateTime(sec);
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
    protected abstract boolean autoOutCard(int sec);

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
                BaseMJRoom_RoundPos data = new BaseMJRoom_RoundPos();
                boolean isSelf = pos == roundPos.getOpPos();
                data.setOpList(roundPos.getRecieveOpTypes());
                data.setChiList(roundPos.getPos().getPosOpNotice().getChiList());
                data.setLastOpCard(roundPos.getLastOutCard());
                data.setWaitOpPos(roundPos.getOpPos());
                data.setTingCardMap(isSelf ? roundPos.getPos().getPosOpNotice().getTingCardMap() : null);
                if (this.isBuChiFuDaFu()) {
                    data.setBuChuList(isSelf ? roundPos.getPos().getPosOpNotice().getBuNengChuList() : null);
                }
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

    /**
     * 是否可以吃幅打幅
     *
     * @return T:不能吃幅打幅，F:可以
     */
    protected boolean isBuChiFuDaFu() {
        return false;
    }


    /**
     * 机器人操作
     *
     * @param posID
     */
    public void RobothandCrad(int posID) {
        if (this.getEndTime() > 0) {
            return;
        }
        if (this.getRoundPosDict().containsKey(posID)) {
            new MJRobotOpCard(this).RobothandCrad(posID);
        }
    }

    /**
     * 获取本回合开始时间
     *
     * @return
     */
    public int getStartTime() {
        return startTime;
    }

    /**
     * 获取玩家操作位置
     *
     * @return
     */
    public int getExeOpPos() {
        return exeOpPos;
    }

    /**
     * 设置玩家操作位置
     *
     * @param exeOpPos
     */
    public void setExeOpPos(int exeOpPos) {
        this.exeOpPos = exeOpPos;
    }

    /**
     * 下位置操作类型
     *
     * @param nextPos
     * @return
     */
    public AbsMJRoundPos nextPosOpType(AbsMJRoundPos nextPos) {
        if (nextPos.getPos().checkOpType(0, OpType.TingYouJin)) {
            nextPos.addOpType(OpType.TingYouJin);
        } else {
            if (nextPos.getPos().checkOpType(0, OpType.Ting)) {
                nextPos.addOpType(OpType.Ting);
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

    /**
     * 下回合操作位置
     *
     * @param pos
     * @return
     */
    protected abstract AbsMJRoundPos nextRoundPos(int pos);

    /**
     * 开始本回合,并摸牌
     *
     * @param pos
     * @param isNormalMo
     * @return
     */
    public boolean startWithGetCard(int pos, boolean isNormalMo) {
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
     * 本回合的操作玩
     *
     * @param pos 位置ID
     * @return
     */
    protected boolean MJRoundPos(int pos) {
        AbsMJRoundPos tmPos = this.nextRoundPos(pos);
        tmPos.addOpType(tmPos.getPos().recieveOpTypes());
        this.roundPosDict.put(tmPos.getOpPos(), tmPos);
        return true;
    }

    /**
     * 检查位置操作列表
     *
     * @param opType
     * @return
     */
    public OpType checkPosList(OpType opType) {
        if (!HuType.NotHu.equals(MJCEnum.OpHuType(opType)) || opType == OpType.Pass || opType == OpType.Out) {
            return opType;
        }
        return null;
    }

    // 尝试开始回合, 如果失败，则set结束
    public boolean tryStartRound() {

        AbsMJSetRound preRound = getPreRound(); // 前一个可参考的操作round
        // 第一次，庄家作为操作者，抓牌，等待出牌
        if (null == preRound) {
            int opPos = this.set.getDPos();
            if (!startWithGetCard(opPos, true)) {
                return false;
            }
            this.notifyStart();
            return true;
        }

        // 上轮出牌
        if (preRound.getOpType() == OpType.Out) {
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
     * 尝试开始其他回合 如果 没有其他特殊回合 默认返回 false 否则 对其他特殊操作类型进行操作检查
     *
     * @param preRound 上回合
     * @return
     */
    protected abstract boolean tryStartRoundOther(AbsMJSetRound preRound);

    // / ==================================================


    /**
     * 检查下回合操作位置
     *
     * @param preRound 前回合
     * @return
     */
    public boolean checkNextRoundOpPos(AbsMJSetRound preRound) {
        NextRoundOpPos<AbsMJSetRound> curOpPos = preRound.removeNextRountOp();
        curOpPos.getGetPosOpTypeListMap().entrySet().stream().forEach(entrySet -> {
            AbsMJRoundPos nextPos = this.nextRoundPos(entrySet.getKey());
            nextPos.addOpType(entrySet.getValue());
            nextPos.addOpType(OpType.Pass);
            this.roundPosDict.put(nextPos.getOpPos(), nextPos);
        });
        this.waitDealRound = preRound; // 本轮，接手处理 preRound
        this.notifyStart();
        return true;
    }

    /**
     * 打牌
     *
     * @param preRound
     * @return
     */
    protected boolean tryStartRoundOut(AbsMJSetRound preRound) {
        int opPos = preRound.getExeOpPos();
        // 出牌对应的接手操作
        if (preRound.checkExistNextRoundOp()) {
            // 检查下回合操作位置
            return this.checkNextRoundOpPos(preRound);
        }
        // 无人接手
        else {
            // 只能顺序的抓牌，打牌
            opPos = (opPos + 1) % this.room.getPlayerNum();
            if (!startWithGetCard(opPos, true)) {
                return false;
            }
            notifyStart();
            return true;
        }
    }

    /**
     * 碰
     *
     * @param preRound
     * @return
     */
    protected boolean tryStartRoundPeng(AbsMJSetRound preRound) {
        AbsMJRoundPos nextPos = this.nextRoundPos(preRound.getExeOpPos());
        nextPos = nextPosOpType(nextPos);
        this.roundPosDict.put(nextPos.getOpPos(), nextPos);
        notifyStart();
        return true;
    }

    /**
     * 补杠、接杠
     *
     * @param preRound
     * @return
     */
    protected boolean tryStartRoundGang(AbsMJSetRound preRound) {
        // 上次的出牌，需要继续处理
        if (preRound.checkExistNextRoundOp()) {
            // 检查下回合操作位置
            return this.checkNextRoundOpPos(preRound);
        } else {
            // 继续抓牌
            int opPos = preRound.getExeOpPos();
            if (!startWithGetCard(opPos, false)) {
                return false;
            }
            notifyStart();
            return true;
        }

    }

    /**
     * 暗杠
     *
     * @param preRound
     * @return
     */
    protected boolean tryStartRoundAnGang(AbsMJSetRound preRound) {
        if (!startWithGetCard(preRound.getExeOpPos(), false)) {
            return false;
        }
        notifyStart();
        return true;
    }

    /**
     * 吃
     *
     * @param preRound
     * @return
     */
    protected boolean tryStartRoundChi(AbsMJSetRound preRound) {
        AbsMJRoundPos nextPos = this.nextRoundPos(preRound.getExeOpPos());
        nextPos = nextPosOpType(nextPos);
        this.roundPosDict.put(nextPos.getOpPos(), nextPos);
        notifyStart();
        return true;
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
            } else if (preRound.getOpType() == OpType.Out) {
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
     * 上回合是过牌后的操作
     */
    public boolean passOther() {
        return false;
    }

    /**
     * 检查是否直接过
     *
     * @return
     */
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
     * 检查平胡
     */
    public boolean checkQtherPing() {
        // 继续抓牌
        int opPos = (this.set.getLastOpInfo().getLastOpPos() + 1) % this.room.getPlayerNum();
        if (!startWithGetCard(opPos, true)) {
            return false;
        }
        notifyStart();
        return true;
    }

    /**
     * 检查抢杠胡
     */
    public boolean checkQtherQiang() {
        // 继续抓牌
        if (!startWithGetCard(set.getLastOpInfo().getLastOpPos(), false)) {
            set.getLastOpInfo().clearLast();
            return false;
        }
        set.getLastOpInfo().clearLast();
        notifyStart();
        return true;
    }

    /**
     * 刷新可胡列表
     */
    protected void refreshHuCardTypes(AbsMJSetPos sPos) {
        // 位置操作牌
        if (OpType.Gang.equals(this.getOpType()) || OpType.AnGang.equals(this.getOpType())
                || OpType.JieGang.equals(this.getOpType())) {
            sPos.calcHuFan();
        }
    }

    /**
     * 位置操作牌
     *
     * @param opPosRet 操作位置
     * @param isFlash  是否动画
     */
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

    /**
     * 过漏
     */
    protected final void passLeak(int opCardID, int endPos) {
        MJCard beginCard = this.set.getMJSetCard().getCardByID(opCardID);
        if (null == beginCard) {
            return;
        }
        int begin = beginCard.getOwnnerPos();
        endPos = endPos - begin > 0 ? endPos : endPos + this.set.getPlayerNum();
        // 漏列表
        List<Integer> leakList = new ArrayList<>();
        for (int j = begin + 1; j < endPos; j++) {
            leakList.add(j % this.set.getPlayerNum());
        }
        this.set.getPosDict().values().stream()
                .filter(k -> leakList.contains(k.getPosID()))
                .collect(Collectors.toList()).forEach(k -> {
                    k.clearPass();
                });
    }


    /**
     * T：同一圈内，没过手（过手是摸牌，吃，碰杠都算），就不能碰漏掉的那张牌；(过圈)。
     *
     * @return
     */
    protected abstract boolean checkExistClearPass();

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
    }

    /**
     * 开始当前回合通知
     *
     * @param roomID       房间ID
     * @param room_SetWait 回合消息
     * @return
     */
    protected abstract <T> BaseSendMsg startRound(long roomID, T room_SetWait);

    /**
     * 位置操作牌
     *
     * @param roomID  房间ID
     * @param pos     位置
     * @param set_Pos 位置信息
     * @param opType  动作类型
     * @param opCard  操作牌
     * @param isFlash 是否动画
     * @return
     */
    protected abstract <T> BaseSendMsg posOpCard(long roomID, int pos, T set_Pos, OpType opType, int opCard,
                                                 boolean isFlash);

    /**
     * 主要是跟打清空使用。 清空打牌信息
     */
    protected void cleanOutCardInfo() {

    }

    /**
     * 获取操作位置
     *
     * @return
     */
    public int getOpPos() {
        int ret = -1;
        for (int pos : this.roundPosDict.keySet()) {
            ret = pos;
            break;
        }
        return ret;
    }


    /**
     * 更新结束时间
     */
    public void updateEndTime() {
        this.setEndTime(CommTime.nowSecond());
        this.setEndTimeMillis(CommTime.nowMS());
    }

}
