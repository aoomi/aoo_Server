package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.club.ClubPlayerInfo;

/**
 * 获取俱乐部玩家信息状态改变
 *
 * @author zaf
 */
public class SClub_PlayerInfoChange extends BaseSendMsg {

    public long clubId;//俱乐部ID
    public ClubPlayerInfo clubPlayerInfo;
    public String clubName;//俱乐部名称

    public static SClub_PlayerInfoChange make(long clubId, String clubName, ClubPlayerInfo clubPlayerInfo) {
        SClub_PlayerInfoChange ret = new SClub_PlayerInfoChange();
        ret.clubId = clubId;
        ret.clubPlayerInfo = clubPlayerInfo;
        ret.clubName = clubName;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}