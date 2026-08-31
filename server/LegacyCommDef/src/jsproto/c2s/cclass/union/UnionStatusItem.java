package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 亲友圈
 */
@Data
public class UnionStatusItem {
    /**
     * 0 全部 1未分配 2管理员 3普通
     */
    private int type;
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
    private String clubName;

    public UnionStatusItem(long clubId, long clubSign , String clubName, int type) {
        this.clubId = clubId;
        this.clubSign = clubSign;
        this.clubName = clubName;
        this.type = type;
    }
    public UnionStatusItem( int type) {
        this.type = type;
    }
}
