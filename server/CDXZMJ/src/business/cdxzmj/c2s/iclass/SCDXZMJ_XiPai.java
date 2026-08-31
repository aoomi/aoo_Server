package business.cdxzmj.c2s.iclass;				
				
import cenum.ClassType;				
import jsproto.c2s.cclass.BaseSendMsg;				
				
				
public class SCDXZMJ_XiPai extends BaseSendMsg {				
    /**				
	 * 				
	 */				
	private static final long serialVersionUID = 1L;				
	public long roomID;				
    public long pid;				
    public ClassType cType;				
    public static SCDXZMJ_XiPai make(long roomID, long pid,ClassType cType) {				
    	SCDXZMJ_XiPai ret = new SCDXZMJ_XiPai();				
        ret.roomID = roomID;				
        ret.pid = pid;				
        ret.cType = cType;				
        return ret;				
    				
				
    }				
}				
