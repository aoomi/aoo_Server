package business.xcpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class SXCPDK_ChangeRoomNum extends BaseSendMsg {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public long roomID;
    public String roomKey;
    public int createType;
    public static SXCPDK_ChangeRoomNum make(long roomID, String roomKey,int createType) {
    	SXCPDK_ChangeRoomNum ret = new SXCPDK_ChangeRoomNum();
        ret.roomID = roomID;
        ret.roomKey = roomKey;
        ret.createType = createType;
        return ret;
    }
}
