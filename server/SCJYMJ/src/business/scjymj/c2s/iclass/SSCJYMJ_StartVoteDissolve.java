package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 发起解散通知
 *
 * @author Administrator
 */
@SuppressWarnings("serial")
public class SSCJYMJ_StartVoteDissolve extends BaseSendMsg {
    // 房间ID
    private long roomID;
    // 发起人
    private int createPos;
    // 结束时间
    private int endSec;

    public static SSCJYMJ_StartVoteDissolve make(long roomID, int createPos, int endSec) {
        SSCJYMJ_StartVoteDissolve ret = new SSCJYMJ_StartVoteDissolve();
        ret.setRoomID(roomID);
        ret.setCreatePos(createPos);
        ret.setEndSec(endSec);
        return ret;
    }

    public long getRoomID() {
        return roomID;
    }

    public void setRoomID(long roomID) {
        this.roomID = roomID;
    }

    public int getCreatePos() {
        return createPos;
    }

    public void setCreatePos(int createPos) {
        this.createPos = createPos;
    }

    public int getEndSec() {
        return endSec;
    }

    public void setEndSec(int endSec) {
        this.endSec = endSec;
    }

}
