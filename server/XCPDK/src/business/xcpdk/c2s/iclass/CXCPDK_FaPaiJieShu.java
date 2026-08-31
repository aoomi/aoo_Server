package business.xcpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

@SuppressWarnings("serial")
public class CXCPDK_FaPaiJieShu extends BaseSendMsg {
	public long roomID;
	public int pos;

    public static CXCPDK_FaPaiJieShu make(long roomID, int pos) {
        CXCPDK_FaPaiJieShu ret = new CXCPDK_FaPaiJieShu();
        ret.roomID = roomID;
        ret.pos = pos;
        return ret;
    }
}
