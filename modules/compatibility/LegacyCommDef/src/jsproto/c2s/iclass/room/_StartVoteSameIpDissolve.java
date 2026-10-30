package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import lombok.Data;

import java.util.List;

/**
 * 发起解散通知
 *
 * @author Administrator
 */
@SuppressWarnings("serial")
@Data
public class _StartVoteSameIpDissolve extends BaseSendMsg {
    // 房间ID
    private long roomID;
    // 发起人相同Ip玩家列表
    private List<Long> sameIpPidList;
    // 结束时间
    private int endSec;

    public static _StartVoteSameIpDissolve make(long roomID, List<Long> sameIpPidList, int endSec, String gameNameStr) {
        _StartVoteSameIpDissolve ret = new _StartVoteSameIpDissolve();
        ret.setRoomID(roomID);
        ret.setSameIpPidList(sameIpPidList);
        ret.setEndSec(endSec);
        ret.setGameNameStr(gameNameStr);
        return ret;
    }

    public long getRoomID() {
        return roomID;
    }

    public void setRoomID(long roomID) {
        this.roomID = roomID;
    }

    public int getEndSec() {
        return endSec;
    }

    public void setEndSec(int endSec) {
        this.endSec = endSec;
    }

}