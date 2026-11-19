package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 预留值请求参数
 */

@Data
public class CClub_ReservedValueReq extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;


    /**
     * 操作的pid
     */
    private long pid;

    /**
     * 设置的值
     */
    private double value;
    /**
     * 联赛id
     */
    private long unionId;


    public static CClub_ReservedValueReq make(long clubId, long pid) {
        CClub_ReservedValueReq ret = new CClub_ReservedValueReq();
        ret.setClubId(clubId);
        ret.setPid(pid);
        return ret;
    }
}
