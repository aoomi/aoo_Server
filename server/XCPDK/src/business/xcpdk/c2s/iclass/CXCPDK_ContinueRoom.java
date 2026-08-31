package business.xcpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

@SuppressWarnings("serial")
public class CXCPDK_ContinueRoom extends BaseSendMsg {
	public long roomID;
	public int continueType;

    public static CXCPDK_ContinueRoom make(long roomID, int continueType) {
        CXCPDK_ContinueRoom ret = new CXCPDK_ContinueRoom();
        ret.roomID = roomID;
        ret.continueType = continueType;
        return ret;
    }
}
