package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 俱乐部
 */
@Data
public class UnionAlivePointItem {
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
     * 生存积分状态（0:不开启,1:开启）
     */
    private int alivePointStatus;
    /**
     * 生存积分
     */
    private double alivePoint;

    public UnionAlivePointItem(long clubId, long clubSign, String name, int alivePointStatus, double alivePoint) {
        this.clubId = clubId;
        this.clubSign = clubSign;
        this.name = name;
        this.alivePoint = alivePoint;
        this.alivePointStatus = alivePointStatus;
    }
}
