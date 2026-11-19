package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 更新指定成员的淘汰分
 */
@Data
public class SUnion_EliminatePoint extends BaseSendMsg {
    /**
     * 俱乐部Id
     */
    private long clubId;
    /**
     * 成员Pid
     */
    private long pid;
    /**
     * 淘汰分
     */
    private double eliminatePoint;

    /**
     * 联盟状态
     */
    private int unionState;

    public static SUnion_EliminatePoint make(long clubId, long pid, double eliminatePoint, int unionState) {
        SUnion_EliminatePoint ret = new SUnion_EliminatePoint();
        ret.setClubId(clubId);
        ret.setPid(pid);
        ret.setEliminatePoint(eliminatePoint);
        ret.setUnionState(unionState);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }

}
