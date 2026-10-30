package jsproto.c2s.cclass.club;

import lombok.Data;

import java.io.Serializable;

@Data
public class ClubPromotionLevelReportFormItem implements Serializable {
    /**
     * 日期
     */
    private String dateTime;
    /**
     * 局数
     */
    private int setCount;
    /**
     * 赢家数
     */
    private int winner;
    /**
     * 报名费
     */
    private double entryFee;
    /**
     * 消耗钻石
     */
    private int consume;
    /**
     * 输赢比赛分
     */
    private double sportsPointConsume;
    /**
     * 总比赛分
     */
    private double sumSportsPoint;
    /**
     * 桌子数(中至:房间数)
     */
    private int table;
    /**
     * 推广员战绩分成(中至:活跃积分)
     */
    private double promotionShareValue;
    /**
     * 实际报名费 玩家实际出的报名费 存在大赢家情况下不出或者全出
     */
    private double actualEntryFee;
    /**
     * 成员总积分和
     */
    private double zhongZhiTotalPoint;
    /**
     * 最终积分
     */
    private double zhongZhiFinalTotalPoint;


    public static String getItemsName() {
        return "date_time as dateTime,sum(setCount) as setCount,sum(winner) as winner,sum(roomAvgSportsPointConsume) as entryFee,sum(consume) as consume,sum(sportsPointConsume) as sportsPointConsume,sum(sportsPoint) as sumSportsPoint,roomSize as `table`,sum(promotionShareValue) as `promotionShareValue`,sum(roomSportsPointConsume) as actualEntryFee";
    }
}
