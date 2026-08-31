package business.global.pk;

import cenum.PKOpType;

/**
 * 麻将玩家动作检查和操作
 *
 * @author Administrator
 */
public abstract class AbsPKSetOp {

    /**
     * 执行动作类型
     *
     * @param opCard 牌ID
     * @param opType 动作类型
     * @return
     */
    public abstract boolean doOpType(PKOpCard opCard, PKOpType opType);

    /**
     * 检查动作类型
     *
     * @param opCard 牌ID
     * @param opType 动作类型
     * @return
     */
    public abstract boolean checkOpType(PKOpCard opCard, PKOpType opType);

    /**
     * 清空
     */
    public abstract void clear();
}
