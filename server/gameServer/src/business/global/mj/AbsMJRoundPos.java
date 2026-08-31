package business.global.mj;

import business.global.mj.set.MJOpCard;
import cenum.mj.*;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.def.ErrorCode;
import com.ddm.server.websocket.handler.requset.WebSocketRequest;
import jsproto.c2s.cclass.mj.NextOpType;
import jsproto.c2s.cclass.mj.NextRoundOpPos;
import jsproto.c2s.cclass.mj.OpTypeInfo;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Data
public abstract class AbsMJRoundPos {
    /**
     * 当前回合信息
     */
    protected AbsMJSetRound round;
    /**
     * 本局信息
     */
    protected AbsMJSetRoom set;
    /**
     * 当前等待操作玩家的信息
     */
    protected AbsMJSetPos pos;
    /**
     * 当前等待操作的pos
     */
    protected int opPos;
    /**
     * 可接受的操作
     */
    protected List<OpType> recieveOpTypes = Collections.synchronizedList(new ArrayList<>());
    /**
     * 公开在操作，别人可以看到读条（出牌行为是公开的）
     */
    protected boolean publicWait = false;
    /**
     * 最终执行的操作
     */
    protected OpType opType;
    /**
     * 操作的牌
     */
    protected int opCard;
    /**
     * 玩家操作信息管理
     */
    protected AbsMJSetPosMgr setPosMgr = null;

    public AbsMJRoundPos(AbsMJSetRound round, int opPos) {
        this.round = round;
        this.opPos = opPos;
        this.set = round.getSet();
        this.setPosMgr = set.getSetPosMgr();
        this.pos = set.getMJSetPos(opPos);
        this.RobotLatelyOutCardTime();
    }

    public void clear() {
        this.round = null;
        this.set = null;
        this.pos = null;
        this.recieveOpTypes = null;
        this.opType = null;
        this.setPosMgr = null;
    }


    /**
     * 本回合位置操作 添加动作类型列表
     *
     * @param recieveOpTypes
     */
    public void addOpType(List<OpType> recieveOpTypes) {
        if (CollectionUtils.isEmpty(recieveOpTypes)) {
            return;
        }
        this.recieveOpTypes.addAll(recieveOpTypes);
        this.publicWait = this.recieveOpTypes.contains(OpType.Out);
    }

    /**
     * 本回合位置操作 添加动作类型
     *
     * @param opType
     */
    public void addOpType(OpType opType) {
        if (Objects.isNull(opType)) {
            return;
        }
        this.recieveOpTypes.add(opType);
        this.publicWait = this.recieveOpTypes.contains(OpType.Out);
    }


    /**
     * 本回合位置操作 检查某个动作类型是否存在
     *
     * @param opType 动作类型
     * @return
     */
    public boolean checkRecieveOpTypes(OpType opType) {
        return getRecieveOpTypes().contains(opType);
    }

    /**
     * 通过CardID,获取指定的牌
     *
     * @param cardID
     * @return
     */
    public MJCard getCardByID(int cardID) {
        return set.getMJSetCard().getCardByID(cardID);
    }

    /**
     * 检查指定的牌是否可以打出
     *
     * @param card 牌
     * @return
     */
    public boolean outCard(MJCard card) {
        return pos.outCard(card);
    }

    /**
     * 刷新胡牌列表
     */
    public void tingCard() {
        pos.calcHuFan();
    }

    /**
     * 最后一次操作的牌
     *
     * @return
     */
    public int getLastOpCard() {
        return this.set.getLastOpInfo().getLastOpCard();
    }

    /**
     * 最后一次打出的牌
     *
     * @return
     */
    public int getLastOutCard() {
        return this.set.getLastOpInfo().getLastOutCard();
    }

    /**
     * 接手并清除上次打出的牌
     */
    public void clearLastOutCard() {
        this.set.getLastOpInfo().clearLastOutCard();
    }

    /**
     * 检查本回合是否结束
     *
     * @param isHu 是否立即结束本回合
     */
    public boolean tryEndRound(boolean isHu) {
        return this.round.tryEndRound(isHu);
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
     * 本回合位置操作 设置是否公开等待的动作
     *
     * @param publicWait
     */
    public void setPublicWait(boolean publicWait) {
        if (this.publicWait == publicWait) {
            return;
        }
        this.publicWait = publicWait;
    }


    /**
     * 操作错误
     *
     * @param request
     * @param opType
     * @return
     */
    public int errorOpType(WebSocketRequest request, OpType opType) {
        // 操作错误
        if (!checkRecieveOpTypes(opType)) {
            request.error(ErrorCode.NotAllow, "opType : " + opType);
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
    public abstract int op(WebSocketRequest request, OpType opType, MJOpCard mOpCard);


    /**
     * 记录当前回合操作的牌
     *
     * @param opCard
     */
    public void setOpCard(int opCard) {
        this.opCard = opCard;
        this.round.setOpCard(opCard);
    }

    /**
     * 检查操作返回
     *
     * @param opType      动作类型
     * @param cardID      牌ID
     * @param tryEndRound T:顺序胡，如果顺序最高的点胡，则马上结束本回合 ; F:一炮多响，所有可胡的玩家都操作了，才结束本回合。
     * @return
     */
    protected int opReturn(OpType opType, int cardID, TryEndRoundEnum tryEndRound) {
        // 执行动作类型信息
        OpTypeInfo oInfo = this.set.getSetPosMgr().exeOpTypeInfo(this.opPos, opType);
        if (Objects.isNull(oInfo)) {
            // 如果返回 -3,执行其他动作
            return this.getCardOpPos(opType, cardID);
        }

        if (OpType.Not.equals(oInfo.getOpType())) {
            // 不执行动作
            this.opNotHu(this.opPos, opType, TryEndRoundEnum.ALL_WAIT);
            if (this.tryEndRound(false)) {
                CommLogD.error("RoomID:{},opPos:{},RecieveOpTypes:{}", this.getSet().getRoom().getRoomID(), this.opPos, this.getRecieveOpTypes().toString());
            }
            return MJOpCardError.LINE_UP.value();
        }

        if (OpType.Pass.equals(oInfo.getOpType())) {
            // 过
            this.opNotHu(oInfo.getPosId(), opType, TryEndRoundEnum.ALL_AT_ONCE);
            if (this.tryEndRound(true)) {
                // 记录操作位置
                this.getRound().setExeOpPos(oInfo.getPosId());
            }
            this.setPosMgr.clearOpTypeInfoList();
            return oInfo.getPosId();
        }
        if (this.setPosMgr.checkNotExistOpTypeInfoList()) {
            // 如果返回 -3,执行其他动作
            return this.getCardOpPos(opType, cardID);
        }

        // 动作操作结果
        boolean isRes = this.doOpType(oInfo.getPosId(), this.setPosMgr.opValue(oInfo.getOpType(), oInfo.getPosId()), oInfo.getOpType());
        if (!isRes) {
            // 玩家执行动作有误。
            return MJOpCardError.ERROR_EXEC_OP_TYPE.value();
        }
        // 操作动作类型,并且尝试结束本回合.
        this.opTypeTryEndRound(oInfo.getPosId(), oInfo.getOpType(), MJCEnum.OpHuType(oInfo.getOpType()), tryEndRound);
        if (this.setPosMgr.checkHuEnd()) {
            this.setPosMgr.clearOpTypeInfoList();
        }
        return oInfo.getPosId();
    }

    /**
     * 获取牌的玩家操作。 ----------- 手上有门牌的操作。
     *
     * @param opType 操作类型
     * @param cardID 操作ID
     * @return
     */
    protected abstract int getCardOpPos(OpType opType, int cardID);

    /**
     * 记录操作的动作，并且尝试结束本回合
     *
     * @param posId       操作玩家ID
     * @param opType      操作的动作
     * @param tryEndRound 尝试结束回合状态
     */
    public void opNotHu(int posId, OpType opType, TryEndRoundEnum tryEndRound) {
        this.opTypeTryEndRound(posId, opType, HuType.NotHu, tryEndRound);
    }

    /**
     * 操作动作类型,并且尝试结束本回合
     *
     * @param posId       操作玩家ID
     * @param opType      动作类型
     * @param huType      胡类型
     * @param tryEndRound 尝试结束回合状态
     */
    public void opTypeTryEndRound(int posId, OpType opType, HuType huType, TryEndRoundEnum tryEndRound) {
        // 获取指定用户信息
        AbsMJSetPos oPos = getOpSetPos(posId);
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
        if (!HuType.NotHu.equals(huType)) {
            // 玩家胡了，
            round.setSetHuEnd(true);
            // 记录胡牌玩家胡牌类型和胡牌位置
            oPos.setHuCardType(huType, oPos.getPosID(), this.getRound().getRoundID());
            if (this.round.tryEndRound(this.setPosMgr.checkHuEnd())) {
                this.getRound().setExeOpPos(oPos.getPosID());
            }
            return;
        }
        // 尝试结束本回合
        if (this.tryEndRound(huType, tryEndRound)) {
            this.getRound().setExeOpPos(oPos.getPosID());
        }
    }

    /**
     * 尝试结束本回合
     *
     * @param huType      胡类型
     * @param tryEndRound 尝试结束回合状态
     */
    public boolean tryEndRound(HuType huType, TryEndRoundEnum tryEndRound) {
        if (TryEndRoundEnum.ALL_AT_ONCE.equals(tryEndRound)) {
            return this.round.tryEndRound(true);
        } else if (TryEndRoundEnum.ALL_WAIT.equals(tryEndRound)) {
            return this.round.tryEndRound(false);
        }
        return false;
    }

    /**
     * 动作操作结果
     *
     * @param cardID 牌ID
     * @param opType 动作类型
     * @return
     */
    public boolean doOpType(int cardID, OpType opType) {
        // 动作操作结果
        return this.doOpType(this.opPos, cardID, opType);
    }

    /**
     * 动作操作结果
     *
     * @param opPosID 操作位置ID
     * @param cardID  牌ID
     * @param opType  动作类型
     * @return
     */
    public boolean doOpType(int opPosID, int cardID, OpType opType) {
        // 获取指定用户信息
        AbsMJSetPos oPos = getOpSetPos(opPosID);
        if (null == oPos) {
            return false;
        }
        // 动作操作结果
        return this.doOpType(oPos, cardID, opType);
    }

    /**
     * 获取指定用户信息
     *
     * @param opPosID
     * @return
     */
    public AbsMJSetPos getOpSetPos(int opPosID) {
        if (opPosID == this.opPos) {
            return this.getPos();
        } else {
            return this.set.getMJSetPos(opPosID);
        }
    }

    /**
     * 动作操作结果
     *
     * @param opPosInfo 操作位置信息
     * @param cardID    牌ID
     * @param opType    动作类型
     * @return
     */
    public boolean doOpType(AbsMJSetPos opPosInfo, int cardID, OpType opType) {
        return opPosInfo.doOpType(cardID, opType);
    }

    /**
     * 执行动作
     *
     * @param opType
     * @return
     */
    public int exeCardAction(OpType opType) {
        // 检查动作
        this.set.getSetPosMgr().checkOpType(this.opPos, this.opCard, opType);
        // 获取可指定动作信息
        NextOpType nOpType = this.set.getSetPosMgr().exeCardAction(opType);
        if (Objects.nonNull(nOpType)) {
            // 记录最近操作玩家位置
            set.getLastOpInfo().setLastOpPos(this.opPos);
            if (OpType.Gang.equals(opType)) {
                // 记录抢杠胡，被抢的牌
                set.getLastOpInfo().setLastOpCard(this.opCard);
            } else {
                set.getLastOpInfo().setLastOutCardPos(this.opPos);
            }
            // 下回合操作者
            this.round.addNextRoundOpList(new NextRoundOpPos(nOpType.getPosOpTypeListMap(), this.round));
        }
        // 记录操作的动作，并且尝试结束本回合
        this.opNotHu(this.opPos, opType, TryEndRoundEnum.ALL_WAIT);
        return this.opPos;
    }

    /**
     * 机器人初始时间设置
     */
    protected void RobotLatelyOutCardTime() {
        this.pos.getRoomPos().setLatelyOutCardTime(CommTime.nowMS());
    }


    /**
     * 错误返回值
     *
     * @param request 连接请求
     * @param opType  动作类型
     * @param ret     位置
     * @return
     */
    public int opErrorReturn(WebSocketRequest request, OpType opType, int ret) {
        if (ret < MJOpCardError.SUCCESS.value()) {
            if (ret == MJOpCardError.LINE_UP.value()) {
                // 动作排队中
                request.response();
                return ret;
            }
            // 操作错误
            request.error(ErrorCode.NotAllow, "op :{%s},ret :{%d}", opType.toString(), ret);
            return ret;
        }
        // 返回成功的动作pos位置
        return ret;
    }

    /**
     * 获取操作位置
     *
     * @return
     */
    public int getOpPos() {
        return opPos;
    }

    /**
     * 打牌
     *
     * @param request 连接请求
     * @param opType  动作类型
     * @param cardID  牌值
     * @return
     */
    public int opOutCard(WebSocketRequest request, OpType opType, int cardID) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        // 检查牌是否存在
        MJCard card = getCardByID(cardID);
        if (null == card) {
            request.error(ErrorCode.NotAllow, "1not find cardID:" + cardID);
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 是不是自己身上的牌
        if (!outCard(card)) {
            request.error(ErrorCode.NotAllow, "2not find cardID:" + cardID);
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // =====================================
        // 记录当前回合操作的牌
        this.setOpCard(cardID);
        // 执行动作
        return this.exeCardAction(opType);
    }

    /**
     * 暗杠
     *
     * @param request 连接请求
     * @param opType  动作类型
     * @param cardID  牌值
     * @return
     */
    public int opAnGang(WebSocketRequest request, OpType opType, int cardID) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        // 执行暗杠操作
        if (!doOpType(cardID, opType)) {
            request.error(ErrorCode.NotAllow, "not op_AnGang");
            return MJOpCardError.ERROR_EXEC_OP_TYPE.value();
        }
        // 记录操作的牌ID
        this.setOpCard(cardID);
        // 记录操作的动作，并且尝试结束本回合
        this.opNotHu(this.opPos, opType, TryEndRoundEnum.ALL_WAIT);
        return this.opPos;
    }

    /**
     * 明杠
     *
     * @param request 连接请求
     * @param opType  动作类型
     * @param opCard  牌值
     * @return
     */
    public int opGang(WebSocketRequest request, OpType opType, int opCard) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        // 执行明杠操作
        if (!doOpType(opCard, opType)) {
            request.error(ErrorCode.NotAllow, "not op_Gang");
            return MJOpCardError.ERROR_EXEC_OP_TYPE.value();
        }
        // 记录操作的牌ID
        this.setOpCard(opCard);
        // 记录操作的动作，并且尝试结束本回合
        return this.exeCardAction(opType);
    }

    /**
     * 接杠
     *
     * @param request 连接请求
     * @param opType  动作类型
     * @return
     */
    public int opJieGang(WebSocketRequest request, OpType opType) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        // 设置动作值
        this.setPosMgr.setOpValue(opType, this.getOpPos(), this.getLastOutCard());
        // 执行操作
        return opErrorReturn(request, opType, this.opReturn(opType, 0, TryEndRoundEnum.ALL_AT_ONCE));
    }

    /**
     * 过
     *
     * @param request 连接请求
     * @param opType  动作类型
     * @return
     */
    public int opPass(WebSocketRequest request, OpType opType) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        // 执行操作
        return opErrorReturn(request, opType, this.opReturn(opType, 0, TryEndRoundEnum.ALL_AT_ONCE));
    }

    /**
     * 碰
     *
     * @param request 连接请求
     * @param opType  动作类型
     * @return
     */
    public int opPeng(WebSocketRequest request, OpType opType) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        // 设置动作值
        this.setPosMgr.setOpValue(opType, this.getOpPos(), this.getLastOutCard());
        // 执行操作
        return opErrorReturn(request, opType, this.opReturn(opType, 0, TryEndRoundEnum.ALL_AT_ONCE));
    }

    /**
     * 胡
     *
     * @param request 连接请求
     * @param opType  动作类型
     * @return
     */
    public int opHuType(WebSocketRequest request, OpType opType) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        // 执行操作
        return opErrorReturn(request, opType, this.opReturn(opType, 0, TryEndRoundEnum.ALL_AT_ONCE));
    }


    /**
     * 吃
     *
     * @param request 连接请求
     * @param opType  动作类型
     * @param cardID  吃牌Id
     * @return
     */
    public int opChi(WebSocketRequest request, OpType opType, int cardID) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        // 检查是否有吃牌列表
        if (CollectionUtils.isEmpty(this.pos.getPosOpNotice().getChiList())) {
            request.error(ErrorCode.NotAllow, "not chi");
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 检查是否有指定要吃的牌
        if (!this.pos.getPosOpNotice().getChiList().stream().anyMatch(k -> k.contains(cardID))) {
            request.error(ErrorCode.NotAllow, "isRet not chi");
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 记录操作值
        this.setPosMgr.setOpValue(opType, this.getOpPos(), cardID);
        // 操作返回
        return opErrorReturn(request, opType, this.opReturn(opType, 0, TryEndRoundEnum.ALL_AT_ONCE));
    }

    /**
     * 吃
     *
     * @param request 连接请求
     * @param opType  动作类型
     * @param chiList 吃牌列表
     * @return
     */
    public int opChi(WebSocketRequest request, OpType opType, List<Integer> chiList) {
        // 操作错误
        if (errorOpType(request, opType) <= 0) {
            return MJOpCardError.ERROR_OP_TYPE.value();
        }
        // 检查是否有吃牌列表
        if (CollectionUtils.isEmpty(this.getPos().getPosOpNotice().getChiList()) || CollectionUtils.isEmpty(chiList)) {
            request.error(ErrorCode.NotAllow, "not chi");
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 检查是否有指定要吃的牌
        if (!this.getPos().getPosOpNotice().getChiList().stream().anyMatch(k -> k.contains(chiList.get(0)))) {
            request.error(ErrorCode.NotAllow, "isRet not chi");
            return MJOpCardError.CHECK_OP_TYPE_ERROR.value();
        }
        // 记录操作值
        this.getSetPosMgr().setOpValue(opType, this.getOpPos(), chiList.get(0));
        // 设置吃牌列表
        // 操作返回
        return opErrorReturn(request, opType, this.opReturn(opType, 0, TryEndRoundEnum.ALL_AT_ONCE));
    }

}
