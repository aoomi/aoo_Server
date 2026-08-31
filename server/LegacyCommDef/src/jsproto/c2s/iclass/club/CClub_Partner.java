package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 亲友圈合伙人
 *
 * @author zaf
 */
public class CClub_Partner extends BaseSendMsg {

    public long clubId;//俱乐部ID
    public long pid;//
    public long partnerPid;

    public static CClub_Partner make(long clubId, long pid, long partnerPid) {
        CClub_Partner ret = new CClub_Partner();
        ret.clubId = clubId;
        ret.pid = pid;
        ret.partnerPid = partnerPid;
        return ret;
    }
}