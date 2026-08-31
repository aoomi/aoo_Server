package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 获取房间信息
 *
 * @author Administrator
 */
public class CBase_GetRoomInfo extends BaseSendMsg {
    // 房间ID
    private long roomID;

    public static CBase_GetRoomInfo make(long roomID) {
        CBase_GetRoomInfo ret = new CBase_GetRoomInfo();
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