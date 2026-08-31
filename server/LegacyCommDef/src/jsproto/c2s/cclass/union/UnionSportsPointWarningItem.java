package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 亲友圈
 */
@Data
public class UnionSportsPointWarningItem {
    /**
     * 玩家親友圈ID
     */
    private long clubId;
    /**
     * 玩家
     */
    private long clubSign;
    /**
     * 玩家昵称
     */
    private String name;
    /**
     * 预警状态（0:不预警,1:预警）
     */
    private int warnStatus;
    /**
     * 预警值
     */
    private double sportsPointWarning;

    public UnionSportsPointWarningItem(long clubId, long clubSign, String name, int warnStatus, double sportsPointWarning) {
        this.clubId = clubId;
        this.clubSign = clubSign;
        this.name = name;
        this.warnStatus = warnStatus;
        this.sportsPointWarning = sportsPointWarning;
    }
}
