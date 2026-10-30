package business.cdxzmj.c2s.iclass;				
import jsproto.c2s.cclass.*;				
				
				
@SuppressWarnings("serial")				
public class SCDXZMJ_SetStart<T> extends BaseSendMsg {				
    				
    public long roomID;				
    public T setInfo;				
				
				
    public static <T>SCDXZMJ_SetStart<T> make(long roomID, T setInfo) {				
    	SCDXZMJ_SetStart<T> ret = new SCDXZMJ_SetStart<T>();				
        ret.roomID = roomID;				
        ret.setInfo = setInfo;				
        //打印数组看看				
       				
        return ret;				
    }				
}				
