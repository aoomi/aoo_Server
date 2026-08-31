package jsproto.c2s.cclass.club;

import lombok.Data;

import java.io.Serializable;

@Data
public class ClubAwardDetailZhongZhi implements Serializable {
    /**
     * 序数id
     * 客户端排序用
     */
    private long id;
    /**
     * 大赢家
     */
    private int winner;
    /**
     * 有效耗卡
     */
    private double consume;
    private long clubId;
    private long unionId;
    /**
     * 颁奖次数
     */
    private int awardNum;
    /***
     * 房间配置
     */
    private String configName = "";
    /**
     * 亲友圈成员id
     */
    private long memberId;
    public static String getItemsName() {
        return "winner,consume,clubId,unionId,awardNum,configName,memberId";
    }
    public static String getItemsNameByWinner() {
        return "sum(winner) as winner,sum(consume) as consume,clubId,unionId,awardNum,configName,memberId";
    }
    public static String getItemsNameByConsume() {
        return "sum(winner) as winner,sum(consume) as consume,clubId,unionId,awardNum,configName,memberId";
    }

    public static String getItemsNameByWinnerSelf() {
        return "sum(winner) as winner,sum(consume) as consume,clubId,unionId,configName,memberId";
    }
    public static String getItemsNameByConsumeSelf() {
        return "sum(winner) as winner,sum(consume) as consume,clubId,unionId,configName,memberId";
    }
}
