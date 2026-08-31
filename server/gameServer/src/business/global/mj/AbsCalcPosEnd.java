package business.global.mj;

import cenum.mj.HuType;
import cenum.mj.MJEndType;
import cenum.mj.OpPointEnum;
import com.ddm.server.common.CommLogD;
import com.ddm.server.common.utils.CommMath;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 公共的普通算分方式
 *
 * @author Administrator
 */
@Data
public abstract class AbsCalcPosEnd {
    protected Map<OpPointEnum, Integer> huTypeMap = new HashMap<>();

    // 玩家信息。
    private AbsMJSetPos mSetPos;
    protected int dianGangPoint = 0;

    public int getDianGangPoint() {
        return dianGangPoint;
    }

    public void setDianGangPoint(int dianGangPoint) {
        this.dianGangPoint -= dianGangPoint;
    }

    public AbsCalcPosEnd(AbsMJSetPos mSetPos) {
        this.mSetPos = mSetPos;
    }

    /**
     * 庄闲分数
     *
     * @param isZhuang T：庄家，F：闲家
     * @param params   分数集合计算
     * @return
     */
    public abstract int calcPoint(boolean isZhuang, Object... params);

    /**
     * 计算动作分数类型
     *
     * @param <T>
     */
    public abstract <T> void calcOpPointType(T opType, int count);

    /**
     * 计算动作分数类型
     *
     * @param opType
     * @param calcPoint
     * @param endType
     */
    public <T> void calcOpPointType(T opType, int calcPoint, MJEndType endType) {
        this.addhuType((OpPointEnum) opType, calcPoint, endType);
    }

    /**
     * 动作分数 1V1扣分。
     *
     * @param opType    动作类型
     * @param lastOpPos 输分玩家的位置
     * @param calcPoint 分数
     */
    public void calc1V1Op(int lastOpPos, Object opType, int calcPoint) {
        // 输分玩家的位置信息
        AbsMJSetPos fromPos = this.mSetPos.getMJSetPos(lastOpPos);
        if (null == fromPos) {
            // 没找到
            CommLogD.error("calc1V1Op lastOpPos :{}", lastOpPos);
            return;
        }

        // 赢的分数计算。
        calcOpPointType(opType, calcPoint);
        this.mSetPos.setDeductPoint(this.mSetPos.getDeductPoint() + calcPoint);

        // 输的分数计算。
        fromPos.calcOpPointType(opType, (-calcPoint));
        fromPos.setDeductPoint(fromPos.getDeductPoint() - calcPoint);
    }

    /**
     * 动作分数 1V1扣分。
     *
     * @param opType    动作类型
     * @param lastOpPos 输分玩家的位置
     * @param calcPoint 分数
     */
    public void calc1V1Op(int lastOpPos, Object lastOpType, Object opType, int calcPoint) {
        // 输分玩家的位置信息
        AbsMJSetPos fromPos = this.mSetPos.getMJSetPos(lastOpPos);
        if (null == fromPos) {
            // 没找到
            CommLogD.error("calc1V1Op lastOpPos :{}", lastOpPos);
            return;
        }
        // 赢的分数计算。
        calcOpPointType(opType, calcPoint);
        this.mSetPos.setDeductPoint(this.mSetPos.getDeductPoint() + calcPoint);

        // 输的分数计算。
        fromPos.calcOpPointType(lastOpType, (-calcPoint));
        fromPos.setDeductPoint(fromPos.getDeductPoint() - calcPoint);
    }

    /**
     * 动作分数 1V3扣分。 1人加分，3人扣分
     *
     * @param opType    动作类型
     * @param calcPoint 分数
     */
    public void calc1V3Op(Object opType, int calcPoint) {
        // 其他玩家信息
        AbsMJSetPos mOSetPos = null;
        for (int i = 0; i < this.mSetPos.getPlayerNum(); i++) {
            // 遍历玩家
            mOSetPos = this.mSetPos.getMJSetPos(i);
            if (mOSetPos == null) {
                // 找不到玩家直接跳过。
                continue;
            }
            if (mOSetPos.getPid() == this.mSetPos.getPid()) {
                // 胡牌玩家本身跳过。
                continue;
            } else {
                // 赢的分数计算。
                calcOpPointType(opType, calcPoint);
                this.mSetPos.setDeductPoint(this.mSetPos.getDeductPoint() + calcPoint);

                // 输的分数计算。
                mOSetPos.calcOpPointType(opType, (-calcPoint));
                mOSetPos.setDeductPoint(mOSetPos.getDeductPoint() - calcPoint);
            }
        }
    }

    /**
     * 动作分数 1V3扣分。 1人加分，3人扣分
     *
     * @param weiPaoPos 喂牌位置
     * @param opType    动作类型
     * @param calcPoint 分数
     */
    public void calc1V3Op(int weiPaoPos, Object opType, int calcPoint) {
        // 获取喂牌玩家信息
        AbsMJSetPos weiPaoPosInfo = this.mSetPos.getMJSetPos(weiPaoPos);
        if (null == weiPaoPosInfo) {
            return;
        }
        // 其他玩家信息
        AbsMJSetPos mOSetPos = null;
        for (int i = 0; i < this.mSetPos.getPlayerNum(); i++) {
            // 遍历玩家
            mOSetPos = this.mSetPos.getMJSetPos(i);
            if (mOSetPos == null) {
                // 找不到玩家直接跳过。
                continue;
            }
            if (mOSetPos.getPid() == this.mSetPos.getPid()) {
                // 胡牌玩家本身跳过。
                continue;
            } else {
                // 赢的分数计算。
                calcOpPointType(opType, calcPoint);
                this.mSetPos.setDeductPoint(this.mSetPos.getDeductPoint() + calcPoint);

                // 输的分数计算。
                mOSetPos.calcOpPointType(opType, (-calcPoint));
                weiPaoPosInfo.setDeductPoint(weiPaoPosInfo.getDeductPoint() - calcPoint);
            }
        }
    }

    /**
     * 动作分数 1V3扣分。 1人加分，3人扣分
     *
     * @param excludePos 动作类型
     * @param opType     动作类型
     * @param calcPoint  分数
     */
    public void calc1V2Op(int excludePos, Object opType, int calcPoint) {
        if (excludePos == this.mSetPos.getPosID()) {
            return;
        }
        // 其他玩家信息
        AbsMJSetPos mOSetPos = null;
        for (int i = 1; i < this.mSetPos.getPlayerNum(); i++) {
            int nextPos = (i + excludePos) % this.mSetPos.getPlayerNum();
            // 遍历玩家
            mOSetPos = this.mSetPos.getMJSetPos(nextPos);
            if (mOSetPos == null) {
                // 找不到玩家直接跳过。
                continue;
            }
            if (mOSetPos.getPid() == this.mSetPos.getPid()) {
                // 胡牌玩家本身跳过。
                continue;
            } else {
                // 赢的分数计算。
                calcOpPointType(opType, calcPoint);
                this.mSetPos.setDeductPoint(this.mSetPos.getDeductPoint() + calcPoint);

                // 输的分数计算。
                mOSetPos.calcOpPointType(opType, (-calcPoint));
                mOSetPos.setDeductPoint(mOSetPos.getDeductPoint() - calcPoint);
            }
        }
    }

    /**
     * 1V1扣分。区分庄闲
     * 显示输牌玩家的类型
     *
     * @param lastOpPos  胡牌玩家
     * @param lastOpType 输牌玩家的显示
     * @param opType     动作类型
     * @param params     参数列表[]
     */
    public void calc1V1OpZXLast(int lastOpPos, Object lastOpType, Object opType, Object... params) {
        int calcPoint = 0;
        if (this.mSetPos.getSet().getDPos() == this.mSetPos.getPosID()) {
            // 庄家赢
            calcPoint = calcPoint(true, params);
        } else {
            calcPoint = calcPoint(lastOpPos == this.mSetPos.getSet().getDPos(), params);
        }
        // 胡分数 1V1扣分。
        this.calc1V1Op(lastOpPos, lastOpType, opType, calcPoint);
    }


    /**
     * 1V1扣分。区分庄闲
     *
     * @param lastOpPos 胡牌玩家
     * @param opType    动作类型
     * @param params    参数列表[]
     */
    public void calc1V1OpZX(int lastOpPos, Object opType, Object... params) {
        int calcPoint = 0;
        if (this.mSetPos.getSet().getDPos() == this.mSetPos.getPosID()) {
            // 庄家赢
            calcPoint = calcPoint(true, params);
        } else {
            calcPoint = calcPoint(lastOpPos == this.mSetPos.getSet().getDPos(), params);
        }
        // 胡分数 1V1扣分。
        this.calc1V1Op(lastOpPos, opType, calcPoint);
    }

    /**
     * 1V3扣分。区分庄闲 1人加分，3人扣分 、 * @param Object 动作类型
     *
     * @param params 参数列表[]
     */
    public void calc1V3OpZX(Object opType, Object... params) {
        // 当前玩家是否庄家
        boolean isDPos = this.mSetPos.getSet().getDPos() == this.mSetPos.getPosID();
        // 其他玩家信息
        AbsMJSetPos mOSetPos = null;
        for (int i = 0; i < this.mSetPos.getPlayerNum(); i++) {
            // 遍历玩家
            mOSetPos = this.mSetPos.getMJSetPos(i);
            if (mOSetPos == null) {
                // 找不到玩家直接跳过。
                continue;
            }
            if (mOSetPos.getPid() == this.mSetPos.getPid()) {
                // 胡牌玩家本身跳过。
                continue;
            } else {
                int calcPoint = calcPoint(isDPos ? isDPos : this.mSetPos.getSet().getDPos() == mOSetPos.getPosID(), params);
                // 赢的分数计算。
                calcOpPointType(opType, calcPoint);
                this.mSetPos.setDeductPoint(this.mSetPos.getDeductPoint() + calcPoint);
                // 输的分数计算。
                mOSetPos.calcOpPointType(opType, (-calcPoint));
                mOSetPos.setDeductPoint(mOSetPos.getDeductPoint() - calcPoint);
            }
        }
    }

    /**
     * 添加胡类型
     *
     * @param opPoint
     * @param point
     */
    protected void addhuType(OpPointEnum opPoint, int point, MJEndType bEndType) {
        if (this.huTypeMap.containsKey(opPoint)) {
            // 累计
            int calcPoint = point;
            if (MJEndType.PLUS.equals(bEndType)) {
                calcPoint = this.huTypeMap.get(opPoint) + point;
            } else if (MJEndType.MULTIPLY.equals(bEndType)) {
                calcPoint = this.huTypeMap.get(opPoint) * point;
            }
            this.huTypeMap.put(opPoint, calcPoint);
        } else {
            this.huTypeMap.put(opPoint, point);
        }
    }

    /**
     * 设置点炮用户
     */
    public void setDianPao() {
        AbsMJSetPos aSetPos = this.mSetPos.getMJSetPos(this.mSetPos.getSet().getLastOpInfo().getLastOpPos());
        aSetPos.setHuType(HuType.DianPao);

    }

    /**
     * 玩家当局分数结算
     *
     * @param mSetPos 玩家信息
     */
    public abstract void calcPosEnd(AbsMJSetPos mSetPos);

    /**
     * 玩家当局分数统计
     *
     * @param mSetPos 玩家信息
     */
    public abstract void calcPosPoint(AbsMJSetPos mSetPos);

    /**
     * 获取位置结算信息
     *
     * @return
     */
    public abstract <T> T getCalcPosEnd();

    /**
     * 添加point到huTypeMap
     *
     * @param opPoint
     * @param count
     */
    public void calcOpPointTypeImpl(OpPointEnum opPoint, int count) {
        switch (opPoint) {
            case Not:
                break;
            default:
                this.addhuType(opPoint, count, MJEndType.PLUS);
                break;
        }
    }

    public void calcPosEndYiKao() {
        this.mSetPos.setEndPoint(this.mSetPos.getEndPoint() + this.mSetPos.getDeductPoint());
        if (this.mSetPos.getRoom().calcFenUseYiKao()) {
            this.mSetPos.setDeductEndPoint(CommMath.addDouble(this.mSetPos.getDeductEndPoint(), this.mSetPos.getDeductPointYiKao()));
        } else {
            this.getMSetPos().setDeductEndPoint(CommMath.addDouble(this.getMSetPos().getDeductEndPoint(), CommMath.mul(this.getMSetPos().getDeductPoint(), Math.max(0D, this.getMSetPos().getRoom().getRoomTyepImpl().getSportsDouble()))));
        }

    }
}
