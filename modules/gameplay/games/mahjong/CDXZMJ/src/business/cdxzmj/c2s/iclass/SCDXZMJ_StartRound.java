package business.cdxzmj.c2s.iclass;				
import jsproto.c2s.cclass.*;				
				
				
@SuppressWarnings("serial")				
public class SCDXZMJ_StartRound<T> extends BaseSendMsg {				
    				
    public long roomID;				
    public T room_SetWait;				
				
				
    public static <T>SCDXZMJ_StartRound<T> make(long roomID, T room_SetWait) {				
    	SCDXZMJ_StartRound<T> ret = new SCDXZMJ_StartRound<T>();				
        ret.roomID = roomID;				
        ret.room_SetWait = room_SetWait;				
				
        return ret;				
    				
				
    }				
}				
