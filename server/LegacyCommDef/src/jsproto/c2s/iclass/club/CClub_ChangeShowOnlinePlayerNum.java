package jsproto.c2s.iclass.club;

import lombok.Data;

/**
 * 修改亲友圈加入退出需要审核的功能
 */
@Data
public class CClub_ChangeShowOnlinePlayerNum {
    /**
     * 亲友圈Id
     */
    public long clubId;

    /**
     * 查看在线人数(0:全部可见,1:推广员不可见)
     */
    private int showOnlinePlayerNum = 0;


}
