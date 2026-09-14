package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 联盟邀请信息
 */
@Data
public class UnionInvitedInfo {
    /**
     * 随机的联盟标识ID
     */
    private long unionId;
    /**
     * 俱乐部Id
     */
    public long clubId;
    /**
     * 随机的联盟标识ID-6为标识
     */
    private int unionSign;
    /**
     * 联盟名称
     */
    private String unionName;

    public UnionInvitedInfo(long unionId, long clubId, int unionSign, String unionName) {
        super();
        this.unionId = unionId;
        this.clubId = clubId;
        this.unionSign = unionSign;
        this.unionName = unionName;
    }


}
