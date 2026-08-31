package jsproto.c2s.iclass.union;

import lombok.Data;

/**
 * 联赛钻石提醒数量修改
 */
@Data
public class CUnion_ChangeDiamondsAttention {
    /**
     * 亲友圈Id
     */
    public long clubId;

    /**
     * 俱乐部管理员钻石提醒
     */
    private int unionDiamondsAttentionMinister = 0;
    /**
     * 俱乐部全员钻石提醒
     */
    private int unionDiamondsAttentionAll = 0;
    /**
     * 联盟id
     */
    public long unionId;

    public static CUnion_ChangeDiamondsAttention make(long clubId, int unionDiamondsAttentionAll, int unionDiamondsAttentionMinister, long unionId) {
        CUnion_ChangeDiamondsAttention ret = new CUnion_ChangeDiamondsAttention();
        ret.clubId = clubId;
        ret.unionDiamondsAttentionAll = unionDiamondsAttentionAll;
        ret.unionDiamondsAttentionMinister = unionDiamondsAttentionMinister;
        ret.unionId = unionId;
        return ret;
    }
}
