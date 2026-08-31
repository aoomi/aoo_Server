package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

@SuppressWarnings("serial")
public class SSCJYMJ_SendGift extends BaseSendMsg {
    public long roomID;
    public int sendPos; //发送者
    public int recivePos; //接受者
    public long productId;

    public static SSCJYMJ_SendGift make(long roomID, int sendPos, int recivePos, long productId) {
        SSCJYMJ_SendGift ret = new SSCJYMJ_SendGift();
        ret.roomID = roomID;
        ret.sendPos = sendPos;
        ret.recivePos = recivePos;
        ret.productId = productId;
        return ret;
    }
}
