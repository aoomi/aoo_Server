package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 解散房间
 *
 * @author Administrator
 */
public class CBase_DissolveRoom extends BaseSendMsg {
    // 房间ID
    private long roomID;

    public static CBase_DissolveRoom make(long roomID) {
        CBase_DissolveRoom ret = new CBase_DissolveRoom();
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