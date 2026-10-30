package jsproto.c2s.cclass.club;

import lombok.Data;

@Data
public class ClubLevelRoomCountLogItem {
    private String date_time;
    private int setCount;
    private int winner;
    private int consume;
    private long upLevelId;
    private long memberId;
    private long sumPoint;
    private double sportsPoint = 0D;
    private double sportsPointConsume;
    private double roomSportsPointConsume;
    private double roomAvgSportsPointConsume;
    private long clubId;
    private long unionId;
    private double promotionShareValue;

    public static String getItemsName() {
        return "date_time,sum(setCount) as setCount,sum(winner) as winner,sum(consume) as consume,upLevelId,memberId,0 as sumPoint,sum(sportsPoint) as sportsPoint,sum(sportsPointConsume) as sportsPointConsume,sum(roomSportsPointConsume) as roomSportsPointConsume,sum(roomAvgSportsPointConsume) as roomAvgSportsPointConsume,clubId,unionId,sum(promotionShareValue) as promotionShareValue";
    }

    /**
     * 获取联盟所有玩家输赢分总和
     *
     * @return
     */
    public static String getItemsNameByAllUnionWinLose() {
        return "date_time,sum(sportsPointConsume) as sportsPointConsume,sum(consume) as consume,sum(winner) as winner,sum(roomSportsPointConsume) as roomSportsPointConsume,sum(promotionShareValue) as promotionShareValue";
    }
}
