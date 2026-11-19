package business.xcpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * @author zhujianming
 * @date 2022-04-22 10:11
 */
public class SXCPDK_GetTime extends BaseSendMsg {
    // 房间ID
    private long roomID;
    // 玩家PID
    private long pid;
    // 位置
    private int pos;
    public int secTotal = -1;

    public static SXCPDK_GetTime make(long roomID, long pid, int pos) {
        SXCPDK_GetTime ret = new SXCPDK_GetTime();
        ret.setRoomID(roomID);
        ret.setPid(pid);
        ret.setPos(pos);
        return ret;
    }

    public long getRoomID() {
        return roomID;
    }

    public void setRoomID(long roomID) {
        this.roomID = roomID;
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

}
