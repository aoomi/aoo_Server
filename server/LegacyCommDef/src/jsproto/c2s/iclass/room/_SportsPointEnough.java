package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;

public class _SportsPointEnough extends BaseSendMsg {
    //房间id
    public long roomID;
    public String msg;

    public static _SportsPointEnough make(long roomID, String msg, String gameName) {
        _SportsPointEnough ret = new _SportsPointEnough();
        ret.roomID = roomID;
        ret.msg = msg;
        ret.setGameNameStr(gameName);
        return ret;
    }
}
