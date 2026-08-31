package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

@Data
public class CClub_PromotionShareChange extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;


    /**
     * 操作的pid
     */
    private long pid;

    /**
     * 查询
     */
    private double value;
    /**
     * 联赛id
     */
    private long unionId;
    /**
     * 分成类型（0：百分比，1：固定值）
     */
    private int type;

    public static CClub_PromotionShareChange make(long clubId, long pid) {
        CClub_PromotionShareChange ret = new CClub_PromotionShareChange();
        ret.setClubId(clubId);
        ret.setPid(pid);
        return ret;
    }
}
