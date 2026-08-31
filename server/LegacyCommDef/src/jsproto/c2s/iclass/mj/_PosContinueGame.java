package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 位置继续游戏通知
 *
 * @author Administrator
 */
@SuppressWarnings("serial")
public class _PosContinueGame extends BaseSendMsg {
    // 房间ID
    private long roomID;
    // 位置
    private int pos;

    public static _PosContinueGame make(long roomID, int pos, String gameNameStr) {
        _PosContinueGame ret = new _PosContinueGame();
        ret.setRoomID(roomID);
        ret.setPos(pos);
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

}	
