package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;

public class _ChangePlayerNum extends BaseSendMsg {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public int playerNum; // 不用设置为位置 值为-1 否侧为设置固定位置
    public long roomID;
    public int createPos;
    public int endSec;

    public static _ChangePlayerNum make(long roomID, int createPos, int endSec, int playerNum, String gameNameStr) {
        _ChangePlayerNum ret = new _ChangePlayerNum();
        ret.roomID = roomID;
        ret.playerNum = playerNum;
        ret.createPos = createPos;
        ret.endSec = endSec;
        ret.setGameNameStr(gameNameStr);
        return ret;
    }
}	
