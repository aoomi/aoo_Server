package jsproto.c2s.iclass.club;

import lombok.Data;

/**
 * 修改俱乐部加入退出需要审核的功能
 */
@Data
public class CClub_ChangePromotionBelong {
    /**
     * 俱乐部Id
     */
    public long clubId;

    /**
     * 被修改的人的pid
     */
    private int pid;
    /**
     * 修改后归属的id
     */
    private int upLevelId;
    /**
     * 修改类型
     * 0  修改到俱乐部圈主名下
     * 1 修改到目标玩家下
     */
    private int type;

    public static CClub_ChangePromotionBelong make(long clubId, int pid, int upLevelId, int type) {
        CClub_ChangePromotionBelong ret = new CClub_ChangePromotionBelong();
        ret.clubId = clubId;
        ret.pid = pid;
        ret.upLevelId = upLevelId;
        ret.type = type;
        return ret;
    }
}
