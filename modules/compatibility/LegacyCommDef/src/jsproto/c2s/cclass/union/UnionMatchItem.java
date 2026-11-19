package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 联盟比赛项
 */
@Data
public class UnionMatchItem {
    /**
     * 排名
     */
    private int rankingId;
    /**
     * 成员昵称
     */
    private String name;
    /**
     * 成员Id
     */
    private long pid;
    /**
     * 俱乐部Id
     */
    private int clubSign;
    /**
     * 所属俱乐部
     */
    private String clubName;
    /**
     * 比赛分
     */
    private double sportsPoint;
    /**
     * 俱乐部id
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
