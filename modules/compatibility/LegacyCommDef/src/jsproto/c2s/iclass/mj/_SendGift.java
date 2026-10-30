package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;

@SuppressWarnings("serial")
public class _SendGift extends BaseSendMsg {
    public long roomID;
    public int sendPos; //发送者
    public int recivePos; //接受者
    public long productId;

    public static _SendGift make(long roomID, int sendPos, int recivePos, long productId, String gameNameStr) {
        _SendGift ret = new _SendGift();
        ret.roomID = roomID;
        ret.sendPos = sendPos;
        ret.recivePos = recivePos;
        ret.productId = productId;
        ret.setGameNameStr(gameNameStr);
        return ret;
    }
}	
