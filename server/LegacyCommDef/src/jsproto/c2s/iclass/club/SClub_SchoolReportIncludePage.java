package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.club.ClubPlayerRoomAloneLogBO;

import java.util.List;

/**
 * 页数信息
 *
 * @author zaf
 */
public class SClub_SchoolReportIncludePage extends BaseSendMsg {

    public int totalPageNum;//总页数
    public List<ClubPlayerRoomAloneLogBO> clubPlayerRoomAloneLogBOS;//战绩列表

    public static SClub_SchoolReportIncludePage make(int totalPageNum, List<ClubPlayerRoomAloneLogBO> clubPlayerRoomAloneLogBOS) {
        SClub_SchoolReportIncludePage ret = new SClub_SchoolReportIncludePage();
        ret.totalPageNum = totalPageNum;
        ret.clubPlayerRoomAloneLogBOS = clubPlayerRoomAloneLogBOS;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}