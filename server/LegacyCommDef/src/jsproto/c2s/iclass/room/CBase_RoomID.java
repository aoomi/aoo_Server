package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 房间ID
 *
 * @author Administrator
 */
public class CBase_RoomID extends BaseSendMsg {
    // 房间ID
    private long roomID;

    public static CBase_RoomID make(long roomID) {
        CBase_RoomID ret = new CBase_RoomID();
        ret.setRoomID(roomID);
        return ret;

    }

    public long getRoomID() {
        return roomID;
    }

    public void setRoomID(long roomID) {
        this.roomID = roomID;
    }
}