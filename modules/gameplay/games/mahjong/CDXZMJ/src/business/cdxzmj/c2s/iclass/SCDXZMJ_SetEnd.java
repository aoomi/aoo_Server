package business.cdxzmj.c2s.iclass;				
import jsproto.c2s.cclass.*;				
				
				
@SuppressWarnings("serial")				
public class SCDXZMJ_SetEnd<T> extends BaseSendMsg {				
    				
    public long roomID;				
    public T setEnd;				
				
				
    public static <T>SCDXZMJ_SetEnd<T> make(long roomID, T setEnd) {				
    	SCDXZMJ_SetEnd<T> ret = new SCDXZMJ_SetEnd<T>();				
        ret.roomID = roomID;				
        ret.setEnd = setEnd;				
				
        return ret;				
    				
				
    }				
}				
