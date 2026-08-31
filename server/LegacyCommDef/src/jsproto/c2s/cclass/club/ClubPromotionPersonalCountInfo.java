package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * 亲友圈推广员指定下属战绩统计查看
 */
@Data
public class ClubPromotionPersonalCountInfo {
    /**
     * 玩家Id
     */
    private long pid;
    /**
     * 玩家昵称
     */
    private String name;
    /**
     * 大赢家次数
     */
    private int winner;


    public static String getItemsName() {
        return "pid, "
                + "sum(winner) as winner";
    }
}
