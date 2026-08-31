package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class SNJPDK_AddBombScore extends BaseSendMsg {

    public long roomID;
    public int pos;  //位置
    public boolean lastHand = false;

    public static SNJPDK_AddBombScore make(long roomID, int pos, boolean lastHand) {
        SNJPDK_AddBombScore ret = new SNJPDK_AddBombScore();
        ret.pos = pos;
        ret.roomID = roomID;
        ret.lastHand = lastHand;
        return ret;
    }
}
