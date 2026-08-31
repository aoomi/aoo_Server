package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;


@SuppressWarnings("serial")
public class _PosGetCard<T> extends BaseSendMsg {

    public long roomID;
    public int pos;
    public int normalMoCnt;
    public int gangMoCnt;
    public T set_Pos;


    public static <T> _PosGetCard<T> make(long roomID, int pos, int normalMoCnt, int gangMoCnt, T set_Pos, String gameNameStr) {
        _PosGetCard<T> ret = new _PosGetCard<T>();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.normalMoCnt = normalMoCnt;
        ret.gangMoCnt = gangMoCnt;
        ret.set_Pos = set_Pos;
        ret.setGameNameStr(gameNameStr);
        return ret;


    }
}	
