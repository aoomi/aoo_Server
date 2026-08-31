package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.Player.ShortPlayer;

/**
 * 亲友圈操作信息
 *
 * @author zaf
 */
public class SClub_GroupingInfo extends BaseSendMsg {

    public long clubId;//俱乐部ID
    public long groupingId;//分组ID
    public ShortPlayer player;

    public static SClub_GroupingInfo make(long clubId, long groupingId, ShortPlayer player) {
        SClub_GroupingInfo ret = new SClub_GroupingInfo();
        ret.clubId = clubId;
        ret.groupingId = groupingId;
        ret.player = player;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}