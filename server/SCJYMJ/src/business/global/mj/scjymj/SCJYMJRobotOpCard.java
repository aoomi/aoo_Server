package business.global.mj.scjymj;

import business.global.mj.AbsMJRoundPos;
import business.global.mj.AbsMJSetPos;
import business.global.mj.AbsMJSetRound;
import business.global.mj.robot.MJRobotOpCard;
import business.global.mj.set.MJOpCard;
import cenum.PrizeType;
import cenum.mj.HuType;
import cenum.mj.MJCEnum;
import cenum.mj.OpType;
import com.ddm.server.common.utils.CommMath;
import com.ddm.server.common.utils.CommTime;
import com.ddm.server.websocket.handler.requset.WebSocketRequestDelegate;

import java.util.List;

/**
 * 麻将机器人打牌
 *
 * @author Huaxing
 */
public class SCJYMJRobotOpCard extends MJRobotOpCard {

    public SCJYMJRobotOpCard(AbsMJSetRound setRound) {
        super(setRound);
    }


    public void RobothandCrad(int posID) {
        if (CommTime.nowSecond() - this.getSetRound().getStartTime() <= 1) {
            return;
        }
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


        // 操作结果
        int opCardRet = null == mSetPos.getHandCard() ? this.notExistHandCard(opTypes, mSetPos) : this.existHandCard(opTypes, mSetPos);
        if (opCardRet >= 0) {
            // 操作成功可以清除动作列表
            mSetPos.getPosOpRecord().cleanOpList();
        }
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
                this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opType, MJOpCard.OpCard(mSetPos.getSetPosRobot().getAnGangCid()));
            } else if (OpType.Gang.equals(opType)) {
                // 明杠
                this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opType, MJOpCard.OpCard(mSetPos.getSetPosRobot().getGangCid()));
            } else if (OpType.Wan.equals(opType) || OpType.Tiao.equals(opType) || OpType.Tong.equals(opType)) {
                this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opType, MJOpCard.OpCard(0));
            } else {
                if (opTypes.contains(OpType.Out)) {
                    // 打牌
                    this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), OpType.Out, MJOpCard.OpCard(mSetPos.getSetPosRobot().getAutoCard()));
                } else if (opTypes.contains(OpType.Pass)) {
                    // 过操作
                    this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), OpType.Pass, MJOpCard.OpCard(0));
                } else {
                    return -1;
                }
            }
        } else {
            // 存在自摸胡牌
            this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opType, MJOpCard.OpCard(0));
        }
        return 1;
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
                this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opType, MJOpCard.OpCard(0));
            } else if (OpType.Chi.equals(opType)) {
                // 吃牌
                this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opType, MJOpCard.OpCard(mSetPos.getSetPosRobot().getChiCid()));
            } else if (OpType.Wan.equals(opType) || OpType.Tiao.equals(opType) || OpType.Tong.equals(opType)) {
                this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opType, MJOpCard.OpCard(0));
            } else {
                if (opTypes.contains(OpType.Pass)) {
                    // 没有相应的动作直接过
                    this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), OpType.Pass, MJOpCard.OpCard(0));
                } else if (opTypes.contains(OpType.Out)) {
                    this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), OpType.Out, MJOpCard.OpCard(mSetPos.getSetPosRobot().getAutoCard()));
                }
                return -1;

            }
        } else {
            // 点炮胡或者抢杠胡
            this.getSetRound().opCard(new WebSocketRequestDelegate(), mSetPos.getPosID(), opType, MJOpCard.OpCard(0));
        }
        return 1;

    }


    /**
     * 检查超时等待时间
     */
    public void checkWaitTime() {
        if (PrizeType.Gold.equals(this.getSet().getRoom().getBaseRoomConfigure().getPrizeType())) {
            if (CommTime.nowSecond() - this.getSetRound().getStartTime() >= 30) {
                this.getSet().endSet();
            }
        }

    }

}
