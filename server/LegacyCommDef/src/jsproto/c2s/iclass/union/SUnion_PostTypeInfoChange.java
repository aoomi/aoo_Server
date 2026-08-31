package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class SUnion_PostTypeInfoChange extends BaseSendMsg {
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
     * 赛事职务
     */
    private int unionPostType;

    public static SUnion_PostTypeInfoChange make(long unionId, long clubId, int unionPostType, long pid) {
        SUnion_PostTypeInfoChange ret = new SUnion_PostTypeInfoChange();
        ret.setUnionId(unionId);
        ret.setClubId(clubId);
        ret.setUnionPostType(unionPostType);
        ret.setPid(pid);

        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}
