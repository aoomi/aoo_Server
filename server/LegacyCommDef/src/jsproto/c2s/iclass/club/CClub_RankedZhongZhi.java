package jsproto.c2s.iclass.club;

import lombok.Data;

/**
 * 中至排行榜
 */
@Data
public class CClub_RankedZhongZhi {
    private long clubId;
    private long unionId;
    /**
     * 日期 0 今日 1 昨日 2本周 3上周
     */
    private int getType;
    /**
     * 游戏类型
     */
    private int gameType;
    /**
     * 0参与房间数 1参与小局数 2积分 3大赢家 4比赛最高分
     */
    private int type;
    /**
     * 页数
     */
    private int pageNum;
}
