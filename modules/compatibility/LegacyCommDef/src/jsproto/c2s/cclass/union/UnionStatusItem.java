package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 俱乐部
 */
@Data
public class UnionStatusItem {
    /**
     * 0 全部 1未分配 2管理员 3普通
     */
    private int type;
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
