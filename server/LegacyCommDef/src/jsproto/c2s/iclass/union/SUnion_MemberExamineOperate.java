package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 赛事成员审核操作
 *
 * @author zaf
 */
@Data
public class SUnion_MemberExamineOperate extends BaseSendMsg {
    /**
     * 赛事Id
     */
    private long unionId;

    /**
     * 亲友圈Id
     */
    private long clubId;

    /**
     * 玩家Pid
     */
    private long pid;

    /**
     * 0:加入审核,1:退出审核
     */
    private int type;

    /**
     * 0:同意,1:拒绝
     */
    private int operate;

    public static SUnion_MemberExamineOperate make(long unionId, long clubId, int type, int operate) {
        SUnion_MemberExamineOperate ret = new SUnion_MemberExamineOperate();
        ret.setUnionId(unionId);
        ret.setClubId(clubId);
        ret.setType(type);
        ret.setOperate(operate);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }

    public static SUnion_MemberExamineOperate make(long unionId, long clubId, long pid, int type, int operate) {
        SUnion_MemberExamineOperate ret = new SUnion_MemberExamineOperate();
        ret.setUnionId(unionId);
        ret.setClubId(clubId);
        ret.setPid(pid);
        ret.setType(type);
        ret.setOperate(operate);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}