package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 解散房间拒绝
 *
 * @author Administrator
 */
public class CBase_DissolveRoomRefuse extends BaseSendMsg {
    // 房间ID
    private long roomID;

    public static CBase_DissolveRoomRefuse make(long roomID) {
        CBase_DissolveRoomRefuse ret = new CBase_DissolveRoomRefuse();
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