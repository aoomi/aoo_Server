package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.club.ClubInfo;

/**
 * 获取俱乐部链表
 *
 * @author zaf
 */
public class SClub_NewClub extends BaseSendMsg {

    public ClubInfo clubInfo;

    public static SClub_NewClub make(ClubInfo clubInfo) {
        SClub_NewClub ret = new SClub_NewClub();
        ret.clubInfo = clubInfo;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}