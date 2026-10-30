package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 解散房间同意
 *
 * @author Administrator
 */
public class CBase_DissolveRoomAgree extends BaseSendMsg {
    // 房间ID
    private long roomID;

    public static CBase_DissolveRoomAgree make(long roomID) {
        CBase_DissolveRoomAgree ret = new CBase_DissolveRoomAgree();
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