package jsproto.c2s.cclass.union;

import lombok.Data;

/**
 * 赛事邀请信息
 */
@Data
public class UnionInvitedInfo {
    /**
     * 随机的赛事标识ID
     */
    private long unionId;
    /**
     * 亲友圈Id
     */
    public long clubId;
    /**
     * 随机的赛事标识ID-6为标识
     */
    private int unionSign;
    /**
     * 赛事名称
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
