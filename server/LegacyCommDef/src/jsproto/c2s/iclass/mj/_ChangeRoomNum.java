package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;


public class _ChangeRoomNum extends BaseSendMsg {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public long roomID;
    public String roomKey;
    public int createType;

    public static _ChangeRoomNum make(long roomID, String roomKey, int createType, String gameNameStr) {
        _ChangeRoomNum ret = new _ChangeRoomNum();
        ret.roomID = roomID;
        ret.roomKey = roomKey;
        ret.createType = createType;
        ret.setGameNameStr(gameNameStr);
        return ret;
    }
}	
