package jsproto.c2s.cclass.union;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 赛事动态
 */
@Data
@NoArgsConstructor
public class UnionDynamicItemZhongZhiRecord {
    /**
     * 动态ID
     */
    private long id;
    private int execTime;//执行时间
    private int execType;//执行类别
    private String winLoseValue;//比赛输赢
    private String consumeValue;//比赛消耗
    private Double eliminatePoint;//淘汰分
    private String pidCurValue = "";//积分状态

    public UnionDynamicItemZhongZhiRecord(long id, int execTime, int execType, String winLoseValue, String consumeValue, double eliminatePoint, String pidCurValue) {
        this.id = id;
        this.execTime = execTime;
        this.execType = execType;
        this.winLoseValue = winLoseValue;
        this.consumeValue = consumeValue;
        this.eliminatePoint = eliminatePoint;
        this.pidCurValue = pidCurValue;
    }
}
