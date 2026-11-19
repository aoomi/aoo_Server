package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;


public class _ChangePlayerNumAgree extends BaseSendMsg {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public long roomID;
    public int pos;
    public boolean agreeChange;

    public static _ChangePlayerNumAgree make(long roomID, int pos, boolean agreeChange, String gameNameStr) {
        _ChangePlayerNumAgree ret = new _ChangePlayerNumAgree();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.agreeChange = agreeChange;
        ret.setGameNameStr(gameNameStr);
        return ret;


    }
}	
