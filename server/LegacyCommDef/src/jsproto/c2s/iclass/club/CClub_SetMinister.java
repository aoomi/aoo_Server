package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 加入俱乐部
 *
 * @author zaf
 */
public class CClub_SetMinister extends BaseSendMsg {

    public long clubId;//俱乐部编号
    public long pid;//玩家pid
    public int minister;//管理状态

    /**
     * Club_MINISTER_GENERAL (0), //普通成员
     * Club_MINISTER_MGR(1), //管理者
     * Club_MINISTER_CREATER(2), //创建
     */

    public static CClub_SetMinister make(long clubId, long pid, int minister) {
        CClub_SetMinister ret = new CClub_SetMinister();
        ret.clubId = clubId;
        ret.pid = pid;
        ret.minister = minister;
        return ret;
    }
}