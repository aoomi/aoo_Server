package business.global.mj;

import cenum.mj.OpType;

/**
 * 麻将玩家动作检查和操作
 *
 * @author Administrator
 */
public abstract class AbsMJSetOp {

    /**
     * 执行动作类型
     *
     * @param cardID 牌ID
     * @param opType 动作类型
     * @return
     */
    public abstract boolean doOpType(int cardID, OpType opType);

    /**
     * 检查动作类型
     *
     * @param cardID 牌ID
     * @param opType 动作类型
     * @return
     */
    public abstract boolean checkOpType(int cardID, OpType opType);

    /**
     * 清空
     */
    public abstract void clear();
}
