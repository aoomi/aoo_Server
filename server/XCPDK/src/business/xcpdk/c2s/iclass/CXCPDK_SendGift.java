package business.xcpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

@SuppressWarnings("serial")
public class CXCPDK_SendGift extends BaseSendMsg {
	public long roomID;
	public int pos;
	public long productId;

    public static CXCPDK_SendGift make(long roomID, int pos, long productId) {
    	CXCPDK_SendGift ret = new CXCPDK_SendGift();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.productId = productId;
        return ret;
    }
}
