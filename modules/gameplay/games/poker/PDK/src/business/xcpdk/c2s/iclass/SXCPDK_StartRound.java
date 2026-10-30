package business.xcpdk.c2s.iclass;
import jsproto.c2s.cclass.*;


@SuppressWarnings("serial")
public class SXCPDK_StartRound<T> extends BaseSendMsg {
    
    public long roomID;
    public T room_SetWait;


    public static <T>SXCPDK_StartRound<T> make(long roomID, T room_SetWait) {
    	SXCPDK_StartRound<T> ret = new SXCPDK_StartRound<T>();
        ret.roomID = roomID;
        ret.room_SetWait = room_SetWait;

        return ret;
    

    }
}
