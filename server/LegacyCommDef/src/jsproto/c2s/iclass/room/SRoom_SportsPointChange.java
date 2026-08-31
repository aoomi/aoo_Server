package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

/**
 * 房间竞技点改变
 */
@Data
public class SRoom_SportsPointChange extends BaseSendMsg {
    private long roomID;
    private int posId;
    private long pid;
    private double sportsPoint;

    public static SRoom_SportsPointChange make(long roomID, int posId, long pid, double sportsPoint) {
        SRoom_SportsPointChange ret = new SRoom_SportsPointChange();
        ret.setRoomID(roomID);
        ret.setPosId(posId);
        ret.setPid(pid);
        ret.setSportsPoint(sportsPoint);
        return ret;
    }
}
