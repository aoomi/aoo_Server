package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 位置准备通知
 *
 * @author Administrator
 */
@SuppressWarnings("serial")
public class _PosReadyChg extends BaseSendMsg {
    // 房间ID
    private long roomID;
    // 位置
    private int pos;
    // T:准备，F:取消准备
    private boolean isReady;

    public static _PosReadyChg make(long roomID, int pos, boolean isReady, String gameNameStr) {
        _PosReadyChg ret = new _PosReadyChg();
        ret.setRoomID(roomID);
        ret.setPos(pos);
        ret.setReady(isReady);
        ret.setGameNameStr(gameNameStr);
        return ret;
    }

    public long getRoomID() {
        return roomID;
    }

    public void setRoomID(long roomID) {
        this.roomID = roomID;
    }

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean isReady) {
        this.isReady = isReady;
    }

}	
