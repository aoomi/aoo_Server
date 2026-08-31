package jsproto.c2s.iclass.union;

import lombok.Data;

@Data
public class CUnion_SportsPointWarningChange extends CUnion_Base {


    /**
     * 预警状态（0:不预警,1:预警）
     */
    private int warnStatus;
    /**
     * 设置的值
     */
    private double value;

    /**
     * 操作亲友圈Id
     */
    private long opClubId;
    /**
     * 操作玩家
     */
    private long opPid;

    public static CUnion_SportsPointWarningChange make(long clubId, long pid) {
        CUnion_SportsPointWarningChange ret = new CUnion_SportsPointWarningChange();
        ret.setClubId(clubId);
        return ret;
    }
}
