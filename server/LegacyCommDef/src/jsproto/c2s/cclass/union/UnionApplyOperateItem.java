package jsproto.c2s.cclass.union;

import cenum.ConstEnum;
import lombok.Data;

@Data
public class UnionApplyOperateItem {
    /**
     * 预扣值
     */
    private double finalValue;
    /**
     * 前置值
     */
    private double preValue;
    /**
     * 淘汰分
     */
    private double outPoint;
    /**
     * 资源
     */
    private ConstEnum.ResOpType resOpType;

    public UnionApplyOperateItem(double finalValue, double preValue, double outPoint, ConstEnum.ResOpType resOpType) {
        this.finalValue = finalValue;
        this.preValue = preValue;
        this.outPoint = outPoint;
        this.resOpType = resOpType;
    }
}
