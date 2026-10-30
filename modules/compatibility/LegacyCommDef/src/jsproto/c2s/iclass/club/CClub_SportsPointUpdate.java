package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 执行竞技点更新
 *
 * @author zaf
 */
@Data
public class CClub_SportsPointUpdate extends BaseSendMsg {
    /**
     * 俱乐部ID
     */
    private long clubId;
    /**
     * 操作玩家
     */
    private long opPid;
    /**
     * 操作类型(0:加,1:减)
     */
    private int type;
    /**
     * 操作值(>0)
     */
    private double value;

    public CClub_SportsPointUpdate(long clubId, long opPid, int type, double value) {
        this.clubId = clubId;
        this.opPid = opPid;
        this.type = type;
        this.value = value;
    }

    @Override
    public String toString() {
        return "CClub_SportsPointUpdate{" +
                "clubId=" + clubId +
                ", opPid=" + opPid +
                ", type=" + type +
                ", value=" + value +
                '}';
    }
}