package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 亲友圈合伙人
 *
 * @author zaf
 */
public class CClub_PartnerMemberList extends BaseSendMsg {

    public long clubId;// 俱乐部ID
    public int pageNum;
    public long partnerPid;
    public String query;

    public static CClub_PartnerMemberList make(long clubId, int pageNum, long partnerPid) {
        CClub_PartnerMemberList ret = new CClub_PartnerMemberList();
        ret.clubId = clubId;
        ret.pageNum = pageNum;
        ret.partnerPid = partnerPid;
        return ret;
    }
}