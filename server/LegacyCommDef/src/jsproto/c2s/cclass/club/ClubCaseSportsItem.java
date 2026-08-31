package jsproto.c2s.cclass.club;

import lombok.Data;

/**
 * 亲友圈推广员项
 */
@Data
public class ClubCaseSportsItem {
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 保险箱分数
     */
    private double caseSportsPoint;
    /**
     * 竞技点分数
     */
    private double sportsPoint;

    public ClubCaseSportsItem(long pid, double caseSportsPoint, double sportsPoint) {
        this.pid = pid;
        this.caseSportsPoint = caseSportsPoint;
        this.sportsPoint = sportsPoint;
    }
}
