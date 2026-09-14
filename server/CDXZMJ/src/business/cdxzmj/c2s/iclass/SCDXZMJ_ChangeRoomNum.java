package business.cdxzmj.c2s.iclass;				
				
import jsproto.c2s.cclass.BaseSendMsg;				
				
				
public class SCDXZMJ_ChangeRoomNum extends BaseSendMsg {				
    				
    /**				
	 * 				
	 */				
	private static final long serialVersionUID = 1L;				
	public long roomID;				
    public String roomKey;				
    public int createType;				
    public static SCDXZMJ_ChangeRoomNum make(long roomID, String roomKey,int createType) {				
    	SCDXZMJ_ChangeRoomNum ret = new SCDXZMJ_ChangeRoomNum();				
        ret.roomID = roomID;				
        ret.roomKey = roomKey;				
        ret.createType = createType;				
        return ret;				
    }				
}				
