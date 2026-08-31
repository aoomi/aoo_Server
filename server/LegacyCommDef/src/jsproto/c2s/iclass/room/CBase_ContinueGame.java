package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 继续游戏
 *
 * @author Administrator
 */
public class CBase_ContinueGame extends BaseSendMsg {
    // 房间ID
    private long roomID;

    public static CBase_ContinueGame make(long roomID) {
        CBase_ContinueGame ret = new CBase_ContinueGame();
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