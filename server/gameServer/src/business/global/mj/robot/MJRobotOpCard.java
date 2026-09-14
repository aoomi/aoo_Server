package business.global.mj.robot;

import BaseCommon.CommLog;
import business.global.mj.AbsMJRoundPos;
import business.global.mj.AbsMJSetPos;
import business.global.mj.AbsMJSetRoom;
import business.global.mj.AbsMJSetRound;
import business.global.mj.set.MJOpCard;
import cenum.PrizeType;
import cenum.mj.HuType;
import cenum.mj.MJCEnum;
import cenum.mj.MJOpCardError;
import cenum.mj.OpType;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.handler.requset.WebSocketRequestDelegate;
import lombok.Data;

import java.util.List;

/**
 * 麻将机器人打牌
 *
 * @author Huaxing
 */
@Data
public class MJRobotOpCard {
    private AbsMJSetRound setRound;
    private AbsMJSetRoom set;

    public MJRobotOpCard(AbsMJSetRound setRound) {
        this.setRound = setRound;
        this.set = this.setRound.getSet();
    }

    public void RobothandCrad(int posID) {
        // 获取当前操作位置
        AbsMJRoundPos roundPos = this.getSetRound().getRoundPosDict().get(posID);
        if (null == roundPos) {
            // 检查超时等待时间
            this.checkWaitTime();
            return;
        }
        // 检查位置是否已经操作过
        if (null != roundPos.getOpType()) {
            // 检查超时等待时间
            this.checkWaitTime();
            return;
        }
        // 获取玩家信息
        AbsMJSetPos mSetPos = roundPos.getPos();
        if (mSetPos == null) {
            // 检查超时等待时间
            this.checkWaitTime();
            return;
        }

        // 获取玩家可操作列表
        List<OpType> opTypes = roundPos.getRecieveOpTypes();
        if (opTypes == null || opTypes.size() <= 0) {
            // 检查超时等待时间
            this.checkWaitTime();
            return;
        }
        if (moDa(mSetPos, posID)) {
            //机器人摸打
            return;
        }
        // 操作结果
        int opCardRet = null == mSetPos.getHandCard() ? this.notExistHandCard(opTypes, mSetPos) : this.existHandCard(opTypes, mSetPos);
        if (opCardRet >= 0) {
            // 操作成功可以清除动作列表
            mSetPos.getPosOpRecord().cleanOpList();
        }
    }

    /**
     * 机器人摸打
     *
     * @param mSetPos
     * @param posID
     */
    public boolean moDa(AbsMJSetPos mSetPos, int posID) {
        if (getSet().getRoom().isMoDa()) {
            boolean existPass = mSetPos.getPosOpRecord().getOpList().stream().anyMatch(n -> n == OpType.Pass);
            if (existPass) {
                this.getSetRound().opCard(new WebSocketRequestDelegate(), posID, OpType.Pass, MJOpCard.OpCard(0));
                return true;
            }
            boolean existOut = mSetPos.getPosOpRecord().getOpList().stream().anyMatch(n -> n == OpType.Out);
            if (existOut) {
                this.getSetRound().opCard(new WebSocketRequestDelegate(), posID, OpType.Out, MJOpCard.OpCard(mSetPos.getSetPosRobot().getAutoCard2()));
                return true;
            }
        }
        return false;
    }

    /**
     * 存在首牌
     *
     * @return
     */
    public int existHandCard(List<OpType> opTypes, AbsMJSetPos mSetPos) {
        OpType opType = opTypes.stream().filter(k -> !HuType.NotHu.equals(MJCEnum.OpHuType(k))).findAny().orElse(opTypes.get(CommMath.randomInt(0, opTypes.size() - 1)));
        if (HuType.NotHu.equals(MJCEnum.OpHuType(opType))) {
            if (OpType.AnGang.equals(opType)) {
                // 暗杠
                return this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opType, MJOpCard.OpCard(mSetPos.getSetPosRobot().getAnGangCid()));
            } else if (OpType.Gang.equals(opType)) {
                // 明杠
                return this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opType, MJOpCard.OpCard(mSetPos.getSetPosRobot().getGangCid()));
            } else {
                if (opTypes.contains(OpType.Out)) {
                    // 打牌
                    return this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), OpType.Out, MJOpCard.OpCard(mSetPos.getSetPosRobot().getAutoCard()));
                } else if (opTypes.contains(OpType.Pass)) {
                    // 过操作
                    return this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), OpType.Pass, MJOpCard.OpCard(0));
                }
                return MJOpCardError.ROBOT_OP_ERROR.value();
            }
        } else {
            // 存在自摸胡牌
            return this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opType, MJOpCard.OpCard(0));
        }
    }

    /**
     * 不存在首牌
     *
     * @return
     */
    public int notExistHandCard(List<OpType> opTypes, AbsMJSetPos mSetPos) {
        OpType opType = opTypes.stream().filter(k -> !HuType.NotHu.equals(MJCEnum.OpHuType(k))).findAny().orElse(opTypes.get(CommMath.randomInt(0, opTypes.size() - 1)));
        if (HuType.NotHu.equals(MJCEnum.OpHuType(opType))) {
            if (OpType.JieGang.equals(opType) || OpType.Peng.equals(opType) || OpType.Pass.equals(opType)) {
                // 接杠\碰\过
                return this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opType, MJOpCard.OpCard(0));
            } else if (OpType.Chi.equals(opType) && mSetPos.getSetPosRobot().getChiCid() / 100 < 40) {
                // 吃牌
                return this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opType, MJOpCard.OpCard(mSetPos.getSetPosRobot().getChiCid()));
            } else {
                if (opTypes.contains(OpType.Pass)) {
                    // 没有相应的动作直接过
                    return this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), OpType.Pass, MJOpCard.OpCard(0));
                } else if (opTypes.contains(OpType.Out)) {
                    return this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), OpType.Out, MJOpCard.OpCard(mSetPos.getSetPosRobot().getAutoCard()));
                }
                return MJOpCardError.ROBOT_OP_ERROR.value();

            }
        } else {
            // 点炮胡或者抢杠胡
            return this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opType, MJOpCard.OpCard(0));
        }
    }


    /**
     * 检查超时等待时间
     */
    public void checkWaitTime() {
        if (CommTime.nowSecond() - this.setRound.getStartTime() >= 180) {
            if (PrizeType.Gold.equals(this.getSet().getRoom().getBaseRoomConfigure().getPrizeType())) {
                this.getSet().endSet();
            }
            CommLog.info("RobothandCrad RoomID:{},StartTime:{},EndTime:{},UpdateTime:{}", this.getSet().getRoom().getRoomID(), this.getSetRound().getStartTime(), this.getSetRound().getEndTime(), this.getSetRound().getUpdateTime());
        }
    }

}
