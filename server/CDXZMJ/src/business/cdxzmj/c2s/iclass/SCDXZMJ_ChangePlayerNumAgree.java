package business.cdxzmj.c2s.iclass;				
				
import jsproto.c2s.cclass.BaseSendMsg;				
				
				
public class SCDXZMJ_ChangePlayerNumAgree extends BaseSendMsg {				
    				
    /**				
	 * 				
	 */				
	private static final long serialVersionUID = 1L;				
	public long roomID;				
    public int pos;				
    public boolean agreeChange;				
    public static SCDXZMJ_ChangePlayerNumAgree make(long roomID, int pos, boolean agreeChange) {				
    	SCDXZMJ_ChangePlayerNumAgree ret = new SCDXZMJ_ChangePlayerNumAgree();				
        ret.roomID = roomID;				
        ret.pos = pos;				
        ret.agreeChange = agreeChange;				
        return ret;				
    				
				
    }				
}				
