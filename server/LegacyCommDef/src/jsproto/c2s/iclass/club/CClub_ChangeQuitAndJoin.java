package jsproto.c2s.iclass.club;

import lombok.Data;

/**
 * 修改俱乐部加入退出需要审核的功能
 */
@Data
public class CClub_ChangeQuitAndJoin {
    /**
     * 俱乐部Id
     */
    public long clubId;

    /**
     * 加入
     * 0 需要审核 1不需要审核
     */
    private int joinNeedExamine = 0;
    /**
     * 退出
     * 0 需要审核 1不需要审核
     */
    private int quitNeedExamine = 0;

    public static CClub_ChangeQuitAndJoin make(long clubId, int joinNeedExamine, int quitNeedExamine) {
        CClub_ChangeQuitAndJoin ret = new CClub_ChangeQuitAndJoin();
        ret.clubId = clubId;
        ret.joinNeedExamine = joinNeedExamine;
        ret.quitNeedExamine = quitNeedExamine;
        return ret;
    }
}
