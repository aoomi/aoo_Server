package business.xcpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class SXCPDK_ChangePlayerNumAgree extends BaseSendMsg {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public long roomID;
    public int pos;
    public boolean agreeChange;
    public static SXCPDK_ChangePlayerNumAgree make(long roomID, int pos, boolean agreeChange) {
    	SXCPDK_ChangePlayerNumAgree ret = new SXCPDK_ChangePlayerNumAgree();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.agreeChange = agreeChange;
        return ret;
    

    }
}
