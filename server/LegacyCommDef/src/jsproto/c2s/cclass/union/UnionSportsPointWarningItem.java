package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 俱乐部
 */
@Data
public class UnionSportsPointWarningItem {
    /**
     * 成员親友圈ID
     */
    private long clubId;
    /**
     * 成员
     */
    private long clubSign;
    /**
     * 成员昵称
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
