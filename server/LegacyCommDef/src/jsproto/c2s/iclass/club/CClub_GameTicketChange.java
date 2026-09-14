package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CClub_GameTicketChange extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;
    /**
     * 操作的pid
     */
    private long opPid;
    /**
     * 批量操作时候的pid
     */
    private List<Long> opPidList=new ArrayList<>();

    /**
     * 设置的值
     */
    private int value;
    /**
     * 联赛id
     */
    private long unionId;
    /**
     * 俱乐部ID
     */
    private long opClubId;

    public CClub_GameTicketChange() {
    }

    public static CClub_GameTicketChange make(long clubId, long pid) {
        CClub_GameTicketChange ret = new CClub_GameTicketChange();
        ret.setClubId(clubId);
        ret.setOpPid(pid);
        return ret;
    }

    public CClub_GameTicketChange(long clubId, long opPid, int value, long unionId, long opClubId) {
        this.clubId = clubId;
        this.opPid = opPid;
        this.value = value;
        this.unionId = unionId;
        this.opClubId = opClubId;
    }
}
