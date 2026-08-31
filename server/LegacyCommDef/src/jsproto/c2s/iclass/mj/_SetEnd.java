package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;


@SuppressWarnings("serial")
public class _SetEnd<T> extends BaseSendMsg {

    public long roomID;
    public T setEnd;


    public static <T> _SetEnd<T> make(long roomID, T setEnd, String gameNameStr) {
        _SetEnd<T> ret = new _SetEnd<T>();
        ret.roomID = roomID;
        ret.setEnd = setEnd;
        ret.setGameNameStr(gameNameStr);
        return ret;


    }
}	
