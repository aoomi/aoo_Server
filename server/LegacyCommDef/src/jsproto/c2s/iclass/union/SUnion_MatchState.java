package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class SUnion_MatchState extends BaseSendMsg {
    /**
     * 亲友圈Id
     */
    private long clubId;
    /**
     * 赛事Id
     */
    private long unionId;
    /**
     * 玩家Pid
     */
    private long pid;
    /**
     * 赛事状态
     */
    private int matchState;

    /**
     * 0:加入审核,1:退出审核
     */
    private int type;

    /**
     * 0:同意,1:拒绝
     */
    private int operate;

    public static SUnion_MatchState make(long unionId, long clubId, long pid, int matchState, int type, int operate) {
        SUnion_MatchState ret = new SUnion_MatchState();
        ret.setUnionId(unionId);
        ret.setClubId(clubId);
        ret.setPid(pid);
        ret.setMatchState(matchState);
        ret.setType(type);
        ret.setOperate(operate);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }

    public static SUnion_MatchState make(long unionId, long clubId, long pid, int matchState) {
        SUnion_MatchState ret = new SUnion_MatchState();
        ret.setUnionId(unionId);
        ret.setClubId(clubId);
        ret.setPid(pid);
        ret.setMatchState(matchState);
        ret.setType(-1);
        ret.setOperate(-1);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}
