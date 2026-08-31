package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 赛事成员审核项
 */
@Data
public class UnionMemberExamineItem {
    /**
     * 亲友圈名称
     */
    private String clubName;
    /**
     * 亲友圈id
     */
    private long clubId;
    /***
     * 创建者名称
     */
    private String createName;
    /**
     * 圈主id
     */
    private long createId;
    /**
     * 人数
     */
    private long number;

    /**
     * 亲友圈key
     */
    private int clubSign;

    /**
     * 比赛分
     */
    private double sportsPoint;
    /**
     * 0:加入审核,1:退出审核,2:退赛审核,3:重赛审核
     */
    private int type;

    public UnionMemberExamineItem() {
    }

    public UnionMemberExamineItem(String clubName, long clubId, String createName, long createId, int type, long number, int clubSign) {
        this.clubName = clubName;
        this.clubId = clubId;
        this.createName = createName;
        this.createId = createId;
        this.number = number;
        this.clubSign = clubSign;
        this.type = type;
    }

    public UnionMemberExamineItem(String clubName, long clubId, String createName, long createId, int type, double sportsPoint, int clubSign) {
        this.clubName = clubName;
        this.clubId = clubId;
        this.createName = createName;
        this.createId = createId;
        this.clubSign = clubSign;
        this.sportsPoint = sportsPoint;
        this.type = type;
    }
}
