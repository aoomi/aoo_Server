package jsproto.c2s.iclass.union;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 获取赛事邀请链表
 *
 * @author zaf
 */
@Data
public class SUnion_Invited extends BaseSendMsg {
    /**
     * 操作者Pid
     */
    private Long execPid;
    /**
     * 操作者名称
     */
    private String execName;
    /**
     * 亲友圈名称
     */
    private String clubName;
    /**
     * 亲友圈标识
     */
    private Integer clubSign;
    /**
     * 赛事名称
     */
    private String unionName;
    /**
     * 赛事标识
     */
    private Integer unionSign;
    /**
     * 操作类型
     */
    private int execType;

    /**
     * 亲友圈id
     */
    private long clubId;

    public static SUnion_Invited make(long execPid, String execName, String clubName, int clubSign, String unionName, int unionSign, int execType, long clubId) {
        SUnion_Invited ret = new SUnion_Invited();
        ret.setExecPid(execPid);
        ret.setExecName(execName);
        ret.setClubName(clubName);
        ret.setClubSign(clubSign);
        ret.setUnionName(unionName);
        ret.setUnionSign(unionSign);
        ret.setExecType(execType);
        ret.setClubId(clubId);
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }

    public static SUnion_Invited make(String clubName, int clubSign, String unionName, int unionSign, int execType, long clubId) {
        SUnion_Invited ret = new SUnion_Invited();
        ret.setClubName(clubName);
        ret.setClubSign(clubSign);
        ret.setUnionName(unionName);
        ret.setUnionSign(unionSign);
        ret.setExecType(execType);
        ret.setClubId(clubId);
        return ret;
    }
}