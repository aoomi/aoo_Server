package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 加入俱乐部
 *
 * @author zaf
 */
public class CClub_PlayerStatusChange extends BaseSendMsg {

    public long clubId;//俱乐部编号
    public long pid;//踢出
    public int status;//状态
    public boolean audit;//是不是退出审核页面进来的

    public static CClub_PlayerStatusChange make(long clubId, long pid, int status) {
        CClub_PlayerStatusChange ret = new CClub_PlayerStatusChange();
        ret.clubId = clubId;
        ret.pid = pid;
        ret.status = status;
        return ret;
    }
}