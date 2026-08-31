package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * 亲友圈推广员项
 */
@Data
public class ClubPromotionLevelCountItem {
    /**
     * 玩家数
     */
    private int number;
    /**
     * 局数
     */
    private int setCount;
    /**
     * 赢家数
     */
    private int winner;
    /**
     * 消耗数
     */
    private int consume;
    /**
     * 消耗比赛分
     */
    private double sportsPointConsume;
    /**
     * 房费比赛分
     */
    private double roomSportsPointConsume;
    /**
     * 房费比赛分均值
     */
    private double roomAvgSportsPointConsume;


    public static String getItemsName() {
        return "sum(setCount) as setCount,sum(winner) as winner,sum(consume) as consume,sum(sportsPointConsume) as sportsPointConsume,sum(roomSportsPointConsume) as roomSportsPointConsume,sum(roomAvgSportsPointConsume) as roomAvgSportsPointConsume";
    }
}
