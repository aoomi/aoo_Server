package business.xcpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

@SuppressWarnings("serial")
public class SXCPDK_SendGift extends BaseSendMsg {
	public long roomID;
	public int  sendPos; //发送者
	public int 	recivePos; //接受者
	public long productId;

    public static SXCPDK_SendGift make(long roomID, int sendPos, int recivePos, long productId) {
    	SXCPDK_SendGift ret = new SXCPDK_SendGift();
        ret.roomID = roomID;
        ret.sendPos = sendPos;
        ret.recivePos = recivePos;
        ret.productId = productId;
        return ret;
    }
}
