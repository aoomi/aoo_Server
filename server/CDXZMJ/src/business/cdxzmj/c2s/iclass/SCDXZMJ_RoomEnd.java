package business.cdxzmj.c2s.iclass;				
import jsproto.c2s.cclass.BaseSendMsg;				
import jsproto.c2s.cclass.RoomEndResult;				
				
				
@SuppressWarnings("serial")				
public class SCDXZMJ_RoomEnd<T> extends BaseSendMsg {				
    				
    public T record;				
	public RoomEndResult<?> sRoomEndResult;				
				
    public static <T>SCDXZMJ_RoomEnd<T> make(T record,RoomEndResult<?> sRoomEndResult) {				
    	SCDXZMJ_RoomEnd<T> ret = new SCDXZMJ_RoomEnd<T>();				
        ret.record = record;				
        ret.sRoomEndResult = sRoomEndResult;				
        return ret;				
    				
				
    }				
}				
