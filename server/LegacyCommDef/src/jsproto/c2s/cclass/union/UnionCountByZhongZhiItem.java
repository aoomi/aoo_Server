package jsproto.c2s.cclass.union;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class UnionCountByZhongZhiItem implements Serializable {
    /**
     * 房卡消耗
     */
    private int consumeValue = 0;
    /**
     * 联赛活跃积分(总成本)
     */
    private double prizePool;
    /**
     * 最终积分总和(成员总积分和+活跃度)
     */
    private double finalAllMemberPointTotal;
    /**
     * 成员总积分和(输赢分-房费)
     */
    private double unionAllMemberPointTotal;
    /**
     * 输赢分
     */
    private double allWinLose;
    /**
     * 房费
     */
    private double roomSportsPointConsume;
    /**
     * 活跃积分
     */
    private double promotionShareValue;
    /**
     * 中至最终积分
     */
    private double zhongZhiTotalPoint;
    /**
     * 大赢家数
     */
    private int bigWinner;
    /**
     * 开房数
     */
    private int roomSize = 0;
    /**
     * 对局数
     */
    private int setCount = 0;

    public static String getItemsNameCount() {
        return "sum(consumeValue) as consumeValue,sum(prizePool) as prizePool,sum(roomSize) as roomSize,sum(setCount) as setCount,sum(roomSportsPointConsume) as roomSportsPointConsume";
    }

}
