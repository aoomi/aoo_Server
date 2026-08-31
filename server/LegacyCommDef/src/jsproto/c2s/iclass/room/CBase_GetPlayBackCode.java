package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 离开房间
 *
 * @author Administrator
 */
public class CBase_GetPlayBackCode extends BaseSendMsg {
    // 房间ID
    private long roomId;
    private int tabId;

    public static CBase_GetPlayBackCode make(long roomId, int tabId) {
        CBase_GetPlayBackCode ret = new CBase_GetPlayBackCode();
        ret.roomId = roomId;
        ret.tabId = tabId;
        return ret;
    }

    public long getRoomId() {
        return roomId;
    }

    public void setRoomId(long roomId) {
        this.roomId = roomId;
    }

    public int getTabId() {
        return tabId;
    }
}