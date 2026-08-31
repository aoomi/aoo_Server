package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class CBase_SendGift extends BaseSendMsg {
    public long roomID;
    public int pos;
    public long productId;

    public static CBase_SendGift make(long roomID, int pos, long productId) {
        CBase_SendGift ret = new CBase_SendGift();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.productId = productId;
        return ret;
    }
}