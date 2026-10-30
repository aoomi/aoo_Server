package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CClub_SportsPointWarningChange extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;


    /**
     * 操作的pid
     */
    private long pid;
    /**
     * 预警状态（0:不预警,1:预警）
     */
    private int warnStatus;
    /**
     * 设置的值
     */
    private double value;
    /**
     * 联赛id
     */
    private long unionId;


    public static CClub_SportsPointWarningChange make(long clubId, long pid) {
        CClub_SportsPointWarningChange ret = new CClub_SportsPointWarningChange();
        ret.setClubId(clubId);
        ret.setPid(pid);
        return ret;
    }
}
