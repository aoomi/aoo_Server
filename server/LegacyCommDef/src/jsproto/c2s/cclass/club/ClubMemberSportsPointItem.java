package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * 亲友圈推广员项
 */
@Data
public class ClubMemberSportsPointItem {
    /**
     * 比赛分
     */
    private double sportsPoint;
    /**
     * 允许操作的比赛分
     */
    private double allowSportsPoint;

    public ClubMemberSportsPointItem(double sportsPoint, double allowSportsPoint) {
        this.sportsPoint = sportsPoint;
        this.allowSportsPoint = allowSportsPoint;
    }
}
