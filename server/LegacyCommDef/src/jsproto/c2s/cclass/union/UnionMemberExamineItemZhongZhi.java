package jsproto.c2s.cclass.union;

import jsproto.c2s.cclass.Player;
import lombok.Data;

/**
 * 赛事成员审核项
 */
@Data
public class UnionMemberExamineItemZhongZhi {

    private Player.ShortPlayer shortPlayer;
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
    /**
     * 站队长
     */
    private String upPlayerName = "";
    /**
     * 淘汰积分
     */
    private double eliminatePoint = 0d;

    public UnionMemberExamineItemZhongZhi() {
    }

    public UnionMemberExamineItemZhongZhi(Player.ShortPlayer shortPlayer, String clubName, long clubId, String createName, long createId, int type, double sportsPoint, int clubSign, String upPlayerName, double eliminatePoint) {
        this.shortPlayer = shortPlayer;
        this.clubName = clubName;
        this.clubId = clubId;
        this.createName = createName;
        this.createId = createId;
        this.clubSign = clubSign;
        this.sportsPoint = sportsPoint;
        this.type = type;
        this.upPlayerName = upPlayerName;
        this.eliminatePoint = eliminatePoint;
    }
}
