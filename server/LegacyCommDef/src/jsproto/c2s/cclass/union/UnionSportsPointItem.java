package jsproto.c2s.cclass.union;

import lombok.Data;

@Data
public class UnionSportsPointItem {
    /**
     * 当前值
     */
    private double curValue;
    /**
     * 前置值
     */
    private double preValue;
    /**
     * 所属玩家当前值
     */
    private double pidCurValue;
    /**
     * 所属玩家之前值
     */
    private double pidPreValue;
    /**
     * 执行玩家当前值
     */
    private double execPidCurValue;
    /**
     * 执行玩家之前值
     */
    private double execPidPreValue;

    public UnionSportsPointItem(double curValue, double preValue) {
        this.curValue = curValue;
        this.preValue = preValue;
    }

    public UnionSportsPointItem(double pidCurValue, double pidPreValue, double execPidCurValue, double execPidPreValue) {
        this.pidCurValue = pidCurValue;
        this.pidPreValue = pidPreValue;
        this.execPidCurValue = execPidCurValue;
        this.execPidPreValue = execPidPreValue;
    }

    public UnionSportsPointItem(double curValue, double preValue, double pidCurValue, double pidPreValue, double execPidCurValue, double execPidPreValue) {
        this.curValue = curValue;
        this.preValue = preValue;
        this.pidCurValue = pidCurValue;
        this.pidPreValue = pidPreValue;
        this.execPidCurValue = execPidCurValue;
        this.execPidPreValue = execPidPreValue;
    }
}
