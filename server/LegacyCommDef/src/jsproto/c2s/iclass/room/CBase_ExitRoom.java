package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 离开房间
 *
 * @author Administrator
 */
public class CBase_ExitRoom extends BaseSendMsg {
    // 房间ID
    private long roomID;

    public static CBase_ExitRoom make(long roomID) {
        CBase_ExitRoom ret = new CBase_ExitRoom();
        ret.roomID = roomID;
        return ret;
    }

    public long getRoomID() {
        return roomID;
    }

    public void setRoomID(long roomID) {
        this.roomID = roomID;
    }


}