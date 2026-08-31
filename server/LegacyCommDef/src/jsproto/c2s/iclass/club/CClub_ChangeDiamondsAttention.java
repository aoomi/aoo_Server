package jsproto.c2s.iclass.club;

import lombok.Data;

/**
 * 修改亲友圈加入退出需要审核的功能
 */
@Data
public class CClub_ChangeDiamondsAttention {
    /**
     * 亲友圈Id
     */
    public long clubId;

    /**
     * 俱乐部管理员钻石提醒
     */
    private int diamondsAttentionMinister = 0;
    /**
     * 俱乐部全员钻石提醒
     */
    private int diamondsAttentionAll = 0;

    public static CClub_ChangeDiamondsAttention make(long clubId, int diamondsAttentionAll, int diamondsAttentionMinister) {
        CClub_ChangeDiamondsAttention ret = new CClub_ChangeDiamondsAttention();
        ret.clubId = clubId;
        ret.diamondsAttentionMinister = diamondsAttentionMinister;
        ret.diamondsAttentionAll = diamondsAttentionAll;
        return ret;
    }
}
