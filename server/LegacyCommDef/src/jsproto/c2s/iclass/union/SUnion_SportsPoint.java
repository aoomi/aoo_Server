package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 更新指定玩家的竞技点
 */
@Data
public class SUnion_SportsPoint extends BaseSendMsg {
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 竞技点
     */
    private double sportsPoint;

    /**
     * 赛事状态
     */
    private int unionState;

    public static SUnion_SportsPoint make(long clubId, long pid, double sportsPoint, int unionState) {
        SUnion_SportsPoint ret = new SUnion_SportsPoint();
        ret.setClubId(clubId);
        ret.setPid(pid);
        ret.setSportsPoint(sportsPoint);
        ret.setUnionState(unionState);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }

}
