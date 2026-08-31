package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 准备
 *
 * @author Administrator
 */
public class CBase_ReadyRoom extends BaseSendMsg {
    // 房间ID
    private long roomID;

    public static CBase_ReadyRoom make(long roomID) {
        CBase_ReadyRoom ret = new CBase_ReadyRoom();
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