package jsproto.c2s.cclass.union;

import lombok.Data;

import java.io.Serializable;

/**
 * 联盟房间大赢家消耗
 */
@Data
public class UnionRoomSportsBigWinnerConsumeItem implements Serializable {
    /**
     * 分数
     */
    private double winScore;
    /**
     * 比赛分
     */
    private double sportsPoint;
    /**
     * 联盟成本
     */
    private double sportsPointCost;
}
