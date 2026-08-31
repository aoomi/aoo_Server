package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.RoomEndResult;


@SuppressWarnings("serial")
public class _RoomEnd<T> extends BaseSendMsg {

    public T record;
    public RoomEndResult<?> sRoomEndResult;

    public static <T> _RoomEnd<T> make(T record, RoomEndResult<?> sRoomEndResult, String gameNameStr) {
        _RoomEnd<T> ret = new _RoomEnd<T>();
        ret.record = record;
        ret.sRoomEndResult = sRoomEndResult;
        ret.setGameNameStr(gameNameStr);
        return ret;


    }
}	
