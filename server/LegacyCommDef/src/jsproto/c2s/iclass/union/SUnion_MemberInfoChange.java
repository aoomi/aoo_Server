package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class SUnion_MemberInfoChange extends BaseSendMsg {
    /**
     * 赛事Id
     */
    private long unionId;

    /**
     * 亲友圈Id
     */
    private long clubId;

    /**
     * 竞技点分数
     */
    private double sportsPoint;

    /**
     * 分数百分比
     */
    private double scorePercent;

    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 分成类型
     */
    private int shareType;

    /**
     * 分数百分比
     */
    private double shareValue;

    /**
     * 分数百分比
     */
    private double shareFixedValue;

    public static SUnion_MemberInfoChange make(long unionId, long clubId, double sportsPoint, double scorePercent, long pid) {
        SUnion_MemberInfoChange ret = new SUnion_MemberInfoChange();
        ret.setUnionId(unionId);
        ret.setClubId(clubId);
        ret.setSportsPoint(sportsPoint);
        ret.setScorePercent(scorePercent);
        ret.setPid(pid);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);

        return ret;
    }

    public static SUnion_MemberInfoChange make(long unionId, long clubId, double sportsPoint, double scorePercent, long pid, int shareType, double shareValue, double shareFixedValue) {
        SUnion_MemberInfoChange ret = new SUnion_MemberInfoChange();
        ret.setUnionId(unionId);
        ret.setClubId(clubId);
        ret.setSportsPoint(sportsPoint);
        ret.setScorePercent(scorePercent);
        ret.setPid(pid);
        ret.setShareType(shareType);
        ret.setShareValue(shareValue);
        ret.setShareFixedValue(shareFixedValue);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}
