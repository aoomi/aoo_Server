package jsproto.c2s.iclass.club;

import lombok.Data;

/**
 * 修改亲友圈加入退出需要审核的功能
 */
@Data
public class CClub_ChangeTotalPointShowStatus {
    /**
     * 亲友圈Id
     */
    public long clubId;

    /**
     * 0不显示 1显示
     */
    private int type = 0;


    public static CClub_ChangeTotalPointShowStatus make(long clubId, int type) {
        CClub_ChangeTotalPointShowStatus ret = new CClub_ChangeTotalPointShowStatus();
        ret.clubId = clubId;
        ret.type = type;
        return ret;
    }
}
