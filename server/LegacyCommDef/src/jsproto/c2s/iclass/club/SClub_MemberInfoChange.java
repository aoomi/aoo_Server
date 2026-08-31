package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class SClub_MemberInfoChange extends BaseSendMsg {
    /**
     * 亲友圈Id
     */
    private long clubId;

    /**
     * 竞技点分数
     */
    private double sportsPoint;
    /**
     * 玩家Pid
     */
    private long pid;

    public static SClub_MemberInfoChange make(long clubId, double sportsPoint, long pid) {
        SClub_MemberInfoChange ret = new SClub_MemberInfoChange();
        ret.setClubId(clubId);
        ret.setSportsPoint(sportsPoint);
        ret.setPid(pid);

        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}
