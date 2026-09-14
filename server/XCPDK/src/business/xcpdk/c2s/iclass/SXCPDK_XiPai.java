package business.xcpdk.c2s.iclass;

import cenum.ClassType;
import jsproto.c2s.cclass.BaseSendMsg;


public class SXCPDK_XiPai extends BaseSendMsg {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public long roomID;
    public long pid;
    public ClassType cType;
    public static SXCPDK_XiPai make(long roomID, long pid,ClassType cType) {
    	SXCPDK_XiPai ret = new SXCPDK_XiPai();
        ret.roomID = roomID;
        ret.pid = pid;
        ret.cType = cType;
        return ret;
    

    }
}
