package business.global.pk;

import business.global.pk.robot.PKRobotOpCard;
import business.global.room.base.AbsBaseRoom;
import cenum.PKOpType;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.pk.base.BasePKRoom_RoundPos;
import jsproto.c2s.cclass.pk.base.BasePKRoom_SetRound;
import jsproto.c2s.cclass.pk.base.BasePKSet_Pos;
import lombok.Data;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Data
public abstract class AbsPKSetRound {
    /**
     * 当局信息
     */
    protected AbsPKSetRoom set;
    /**
     * 房间信息
     */
    protected AbsBaseRoom room;
    /**
     * 第几回合
     */
    protected int roundID;
    /**
     * 回合开始时间毫秒级
     */
    protected long startTimeMillis;
    /**
     * 回合开始时间
     */
    protected int startTime;
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
    protected Map<Integer, AbsPKRoundPos> roundPosDict = new ConcurrentHashMap<>(8);
    /**
     * 是否结束本回合
     */
    protected boolean isSetEnd = false;
    /**
     * 是否操作出牌
     */
    protected PKOpType opType = null;
    /**
     * 操作的牌
     */
    protected int opCard = 0;
    /**
     * 等待接受处理的round
     */
    protected AbsPKSetRound waitDealRound = null;
    /**
     * 本回合操作位置
     */
    protected int exeOpPos = -1;
    /**
     * 回合信息记录
     */
    protected BasePKRoom_SetRound ret;
    /**
     * 用来查看是否线程更新
     */
    private int updateTime = 0;

    public AbsPKSetRound(AbsPKSetRoom set, int roundID) {
        this.roundID = roundID;
        this.set = set;
        this.room = set.getRoom();
        this.startTime = CommTime.nowSecond();
        this.startTimeMillis = CommTime.nowMS();
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
     * 获得前一轮
     *
     * @return
     */
    public AbsPKSetRound getPreRound() {
        return this.getSet().getPreRound();
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

    // 尝试开始回合, 如果失败，则set结束
    public boolean tryStartRound() {
        AbsPKSetRound preRound = getPreRound(); // 前一个可参考的操作round
        // 第一次，庄家作为操作者，抓牌，等待出牌
        if (null == preRound) {
            int opPos = this.set.getDPos();
            if (!getOpTypeList(opPos)) {
                return false;
            }
            this.notifyStart();
            return true;
        }
        return true;
    }

    /**
     * 获取可操作的类型
     *
     * @param opPos
     * @return
     */
    public boolean getOpTypeList(int opPos) {
        AbsPKRoundPos tmPos = this.nextRoundPos(opPos);
        tmPos.addOpType(tmPos.getPos().receiveOpTypes());
        this.roundPosDict.put(tmPos.getOpPos(), tmPos);
        if (tmPos.getReceiveOpTypes().size() == 0) {
            return false;
        }
        return true;
    }

    /**
     * 下回合操作位置
     *
     * @param pos
     * @return
     */
    protected abstract AbsPKRoundPos nextRoundPos(int pos);

    /**
     * 操作牌
     *
     * @param opPos   操作位置
     * @param opType  动作类型
     * @param mOpCard 操作牌
     */
    public abstract int opCard(WebSocketRequest request, int opPos, PKOpType opType, PKOpCard mOpCard);


    /**
     * 获取本轮信息
     *
     * @param pos 位置
     * @return
     */

    public BasePKRoom_SetRound getNotify_RoundInfo(int pos) {
        ret = new BasePKRoom_SetRound();
        ret.setWaitID(this.roundID);
        ret.setStartWaitSec(this.startTime);
        ret.setRunWaitSec(CommTime.nowSecond() - this.startTime);
        for (AbsPKRoundPos roundPos : this.roundPosDict.values()) {
            if (roundPos.getOpType() != null) {
                continue;
            }
            // 自己 或 公开
            if (pos == roundPos.getOpPos() || roundPos.isPublicWait()) {
                BasePKRoom_RoundPos data = new BasePKRoom_RoundPos();
                data.setOpList(roundPos.getReceiveOpTypes());
                data.setWaitOpPos(roundPos.getOpPos());
                ret.addOpPosList(data);
                if (this.getRoom().isConnectClearTrusteeship()) {
                    // 重新记录打牌时间
                    roundPos.getPos().getRoomPos().setLatelyOutCardTime(CommTime.nowMS());
                }
                // 设置最后操作时间
                this.set.setLastShotTime(CommTime.nowSecond());

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
    protected void posOpCardRet(int opPosRet, boolean isFlash) {
        int opCardID = 0;
        AbsPKSetPos sPos = this.set.getPKSetPos(opPosRet);
        this.setExeOpPos(opPosRet);
        BasePKSet_Pos posInfoOther = sPos.getNotify(false);
        BasePKSet_Pos posInfoSelf = sPos.getNotify(true);
        this.set.getRoomPlayBack().playBack2Pos(opPosRet, this.posOpCard(this.room.getRoomID(), opPosRet, posInfoSelf, this.getOpType(), opCardID, isFlash), set.getSetPosMgr().getAllPlayBackNotify());
        this.set.getRoom().getRoomPosMgr().notify2ExcludePosID(opPosRet, this.posOpCard(this.room.getRoomID(), opPosRet, posInfoOther, this.getOpType(), opCardID, isFlash));
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
    protected abstract <T> BaseSendMsg posOpCard(long roomID, int pos, T set_Pos, PKOpType opType, int opCard, boolean isFlash);


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
            new PKRobotOpCard(this).RobothandCrad(posID);
        }
    }

    /**
     * 清空
     */
    public void clear() {
        this.roundID = 1;
        this.roundPosDict.clear();
        this.waitDealRound = null;
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
        this.waitDealRound = null;
        this.set = null;
        this.room = null;
        this.ret = null;
    }

}
