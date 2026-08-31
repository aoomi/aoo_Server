package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 合伙人个人战绩查询
 */
public class CClub_PartnerPersonalRecord extends BaseSendMsg {
    /**
     * 亲友圈Id
     */
    public long clubId;
    public int getType;
    /**
     * 推广员id
     */
    public long partnerPid;
    /**
     * 下属id
     */
    public long pid;
    /**
     * 第几页 （每页20条）
     */
    public int pageNum;

    /**
     * @param clubId     亲友圈ID
     * @param partnerPid 合伙人PID
     * @param getType    查询的日期，0：查询今天，1：查询昨天，2：近三天。
     * @param pageNum    第几页。
     * @return
     */
    public static CClub_PartnerPersonalRecord make(long clubId, long partnerPid, long pid, int getType, int pageNum) {
        CClub_PartnerPersonalRecord ret = new CClub_PartnerPersonalRecord();
        ret.clubId = clubId;
        ret.partnerPid = partnerPid;
        ret.pid = pid;
        ret.getType = getType;
        ret.pageNum = pageNum;
        return ret;
    }
}