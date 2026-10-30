package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 房间战绩
 *
 * @author Administrator
 */
public class CBase_RoomRecord extends BaseSendMsg {
    // 房间ID
    private long roomID;

    public static CBase_RoomRecord make(long roomID) {
        CBase_RoomRecord ret = new CBase_RoomRecord();
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