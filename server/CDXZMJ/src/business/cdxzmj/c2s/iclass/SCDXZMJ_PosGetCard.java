package business.cdxzmj.c2s.iclass;				
import jsproto.c2s.cclass.*;				
				
				
@SuppressWarnings("serial")				
public class SCDXZMJ_PosGetCard<T> extends BaseSendMsg {				
    				
    public long roomID;				
    public int pos;				
    public int normalMoCnt;				
    public int gangMoCnt;				
    public T set_Pos;				
				
				
    public static <T> SCDXZMJ_PosGetCard<T> make(long roomID, int pos, int normalMoCnt, int gangMoCnt, T set_Pos) {				
    	SCDXZMJ_PosGetCard<T> ret = new SCDXZMJ_PosGetCard<T>();				
        ret.roomID = roomID;				
        ret.pos = pos;				
        ret.normalMoCnt = normalMoCnt;				
        ret.gangMoCnt = gangMoCnt;				
        ret.set_Pos = set_Pos;				
				
        return ret;				
    				
				
    }				
}				
