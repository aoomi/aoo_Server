package jsproto.c2s.iclass.mj;

import cenum.ClassType;
import jsproto.c2s.cclass.BaseSendMsg;


public class _XiPai extends BaseSendMsg {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public long roomID;
    public long pid;
    public ClassType cType;

    public static _XiPai make(long roomID, long pid, ClassType cType, String gameNameStr) {
        _XiPai ret = new _XiPai();
        ret.roomID = roomID;
        ret.pid = pid;
        ret.cType = cType;
        ret.setGameNameStr(gameNameStr);
        return ret;


    }
}	
