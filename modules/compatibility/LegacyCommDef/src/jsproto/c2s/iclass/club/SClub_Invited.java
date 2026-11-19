package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.club.ClubInvitedInfo;

/**
 * 获取俱乐部邀请链表
 *
 * @author zaf
 */
public class SClub_Invited extends BaseSendMsg {

    public ClubInvitedInfo invitedInfo;

    public static SClub_Invited make(ClubInvitedInfo invitedInfo) {
        SClub_Invited ret = new SClub_Invited();
        ret.invitedInfo = invitedInfo;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}