package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.Player.ShortPlayer;

/**
 * 赛事操作信息
 *
 * @author zaf
 */
public class SUnion_GroupingInfo extends CUnion_Base {
    public long groupingID;//分组ID
    public ShortPlayer player;

    public static SUnion_GroupingInfo make(long unionId, long clubId, long groupingID, ShortPlayer player) {
        SUnion_GroupingInfo ret = new SUnion_GroupingInfo();
        ret.setUnionId(unionId);
        ret.setClubId(clubId);
        ret.groupingID = groupingID;
        ret.player = player;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}