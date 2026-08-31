package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 合伙人个人战绩查询
 */
public class CClub_PartnerAloneRecord extends BaseSendMsg {

    public long clubId;//俱乐部ID
    /**
     * @see jsproto.c2s.cclass.club.Club_define.Club_Record_Get_Type
     */
    public int getType;//获取时间 0今天,1昨天,2最近三天
    public long partnerPid;
    public int pageNum;

    /**
     * @param clubId     亲友圈ID
     * @param partnerPid 合伙人PID
     * @param getType    查询的日期，0：查询今天，1：查询昨天，2：近三天。
     * @param pageNum    第几页。
     * @return
     */
    public static CClub_PartnerAloneRecord make(long clubId, long partnerPid, int getType, int pageNum) {
        CClub_PartnerAloneRecord ret = new CClub_PartnerAloneRecord();
        ret.clubId = clubId;
        ret.partnerPid = partnerPid;
        ret.getType = getType;
        ret.pageNum = pageNum;
        return ret;
    }
}