package business.global.pk;

import cenum.PKOpType;
import cenum.mj.MJOpCardError;
import cenum.mj.TryEndRoundEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Data
public abstract class AbsPKRoundPos {
    /**
     * 当前回合信息
     */
    protected AbsPKSetRound round;
    /**
     * 本局信息
     */
    protected AbsPKSetRoom set;
    /**
     * 当前等待操作玩家的信息
     */
    protected AbsPKSetPos pos;
    /**
     * 当前等待操作的pos
     */
    protected int opPos;
    /**
     * 可接受的操作
     */
    protected List<PKOpType> receiveOpTypes = Collections.synchronizedList(new ArrayList<>());
    /**
     * 公开在操作，别人可以看到读条（出牌行为是公开的）
     */
    protected boolean publicWait = false;
    /**
     * 最终执行的操作
     */
    protected PKOpType opType;
    /**
     * 操作的牌
     */
    protected int opCard;
    /**
     * 玩家操作信息管理
     */
    protected AbsPKSetPosMgr setPosMgr = null;

    public AbsPKRoundPos(AbsPKSetRound round, int opPos) {
        this.round = round;
        this.opPos = opPos;
        this.set = round.getSet();
        this.setPosMgr = set.getSetPosMgr();
        this.pos = set.getPKSetPos(opPos);
        this.RobotLatelyOutCardTime();
    }

    /**
     * 机器人初始时间设置
     */
    protected void RobotLatelyOutCardTime() {
        this.pos.getRoomPos().setLatelyOutCardTime(CommTime.nowMS());
    }

    public void clear() {
        this.round = null;
        this.set = null;
        this.pos = null;
        this.receiveOpTypes = null;
        this.opType = null;
        this.setPosMgr = null;
    }


    /**
     * 本回合位置操作 添加动作类型列表
     *
     * @param receiveOpTypes
     */
    public void addOpType(List<PKOpType> receiveOpTypes) {
        if (CollectionUtils.isEmpty(receiveOpTypes)) {
            return;
        }
        this.receiveOpTypes.addAll(receiveOpTypes);
        this.publicWait = this.receiveOpTypes.contains(PKOpType.Pass);
    }

    /**
     * 本回合位置操作 添加动作类型
     *
     * @param opType
     */
    public void addOpType(PKOpType opType) {
        if (Objects.isNull(opType)) {
            return;
        }
        this.receiveOpTypes.add(opType);
        this.publicWait = this.receiveOpTypes.contains(PKOpType.Pass);
    }

    /**
     * 本回合位置操作 检查某个动作类型是否存在
     *
     * @param opType 动作类型
     * @return
     */
    public boolean checkReceiveOpTypes(PKOpType opType) {
        return getReceiveOpTypes().contains(opType);
    }

    /**
     * 本回合位置操作 是否公开等待的动作
     *
     * @return
     */
    public boolean isPublicWait() {
        return publicWait;
    }

    /**
     * 操作错误
     *
     * @param request
     * @param opType
     * @return
     */
    public int errorOpType(WebSocketRequest request, PKOpType opType) {
        // 操作错误
        if (!checkReceiveOpTypes(opType)) {
            request.error(ErrorCode.NotAllow, "PKOpType : " + opType);
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        return 1;
    }

    /**
     * 操作
     *
     * @param request 通信
     * @param opType  动作类型
     * @param mOpCard 操作牌
     * @return
     */
    public abstract int op(WebSocketRequest request, PKOpType opType, PKOpCard mOpCard);

    /**
     * 获取牌的玩家操作。 ----------- 手上有门牌的操作。
     *
     * @param opType 操作类型
     * @param cardID 操作ID
     * @return
     */
    protected abstract int getCardOpPos(PKOpType opType, int cardID);

    /**
     * 获取指定用户信息
     *
     * @param opPosID
     * @return
     */
    public AbsPKSetPos getOpSetPos(int opPosID) {
        if (opPosID == this.opPos) {
            return this.getPos();
        } else {
            return this.set.getPKSetPos(opPosID);
        }
    }

    /**
     * 执行动作
     *
     * @param opType
     * @return
     */
    public int exeCardAction(PKOpType opType) {
        set.setLastShotTime(CommTime.nowSecond());
        // 记录操作的动作，并且尝试结束本回合
        this.opTypeTryEndRound(this.opPos, opType, TryEndRoundEnum.ALL_WAIT);
        return this.opPos;
    }

    /**
     * 动作操作结果
     *
     * @param opPosID 操作位置ID
     * @param opCard  牌ID
     * @param opType  动作类型
     * @return
     */
    public boolean doOpType(int opPosID, PKOpCard opCard, PKOpType opType) {
        // 获取指定用户信息
        AbsPKSetPos oPos = getOpSetPos(opPosID);
        if (null == oPos) {
            return false;
        }
        // 动作操作结果
        return this.doOpType(oPos, opCard, opType);
    }

    /**
     * 动作操作结果
     *
     * @param opPosInfo 操作位置信息
     * @param opCard    牌ID
     * @param opType    动作类型
     * @return
     */
    public boolean doOpType(AbsPKSetPos opPosInfo, PKOpCard opCard, PKOpType opType) {
        return opPosInfo.doOpType(opCard, opType);
    }

    /**
     * 操作动作类型,并且尝试结束本回合
     *
     * @param posId       操作玩家ID
     * @param opType      动作类型
     * @param tryEndRound 尝试结束回合状态
     */
    public void opTypeTryEndRound(int posId, PKOpType opType, TryEndRoundEnum tryEndRound) {
        // 获取指定用户信息
        AbsPKSetPos oPos = getOpSetPos(posId);
        if (null == oPos) {
            CommLogD.error("setOpType null == oPos ");
            return;
        }
        // 记录本回合操作动作
        this.round.setOpType(opType);
        // 记录本回合当前位置操作动作
        this.opType = opType;
        // 清除最近出手的时间
        this.pos.getRoomPos().setLatelyOutCardTime(0L);
        //是否结束
        isEnd(opType, round);
        //检验是否结束
        // 尝试结束本回合
        if (this.tryEndRound(tryEndRound)) {
            this.getRound().setExeOpPos(oPos.getPosID());
        }
    }

    public abstract boolean isEnd(PKOpType opType, AbsPKSetRound round);

    /**
     * 尝试结束本回合
     *
     * @param tryEndRound 尝试结束回合状态
     */
    public boolean tryEndRound(TryEndRoundEnum tryEndRound) {
        if (TryEndRoundEnum.ALL_AT_ONCE.equals(tryEndRound)) {
            return this.round.tryEndRound(true);
        } else if (TryEndRoundEnum.ALL_WAIT.equals(tryEndRound)) {
            return this.round.tryEndRound(false);
        }
        return false;
    }

}
