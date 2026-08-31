package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 亲友圈
 */
@Data
public class UnionAlivePointItem {
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
