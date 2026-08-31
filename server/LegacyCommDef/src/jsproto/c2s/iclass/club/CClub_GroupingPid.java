package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 亲友圈玩家分组
 *
 * @author zaf
 */
public class CClub_GroupingPid extends BaseSendMsg {

    public long clubId;//俱乐部ID
    public long pid;//指定禁止Pid
    public long groupingId;

    public static CClub_GroupingPid make(long clubId, long pid, long groupingId) {
        CClub_GroupingPid ret = new CClub_GroupingPid();
        ret.clubId = clubId;
        ret.pid = pid;
        ret.groupingId = groupingId;
        return ret;
    }
}