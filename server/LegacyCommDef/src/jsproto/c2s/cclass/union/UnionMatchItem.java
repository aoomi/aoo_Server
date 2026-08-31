package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 赛事比赛项
 */
@Data
public class UnionMatchItem {
    /**
     * 排名
     */
    private int rankingId;
    /**
     * 玩家昵称
     */
    private String name;
    /**
     * 玩家Id
     */
    private long pid;
    /**
     * 亲友圈Id
     */
    private int clubSign;
    /**
     * 所属亲友圈
     */
    private String clubName;
    /**
     * 比赛分
     */
    private double sportsPoint;
    /**
     * 亲友圈id
     */
    private long clubId;


    public UnionMatchItem(int rankingId, String name, long pid, int clubSign, String clubName, double sportsPoint) {
        this.rankingId = rankingId;
        this.name = name;
        this.pid = pid;
        this.clubSign = clubSign;
        this.clubName = clubName;
        this.sportsPoint = sportsPoint;
    }

    public UnionMatchItem() {
    }

    public static String getItemsName() {
        return "rankingId,name,pid,clubSign,clubName,sportsPoint,clubId";
    }

}
