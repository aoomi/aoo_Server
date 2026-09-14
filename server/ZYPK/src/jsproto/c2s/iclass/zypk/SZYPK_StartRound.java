package jsproto.c2s.iclass.zypk;
import jsproto.c2s.cclass.*;


public class SZYPK_StartRound<T> extends BaseSendMsg {
    
    public long roomID;
    public T room_SetWait;


    public static <T>SZYPK_StartRound make(long roomID, T room_SetWait) {
    	SZYPK_StartRound ret = new SZYPK_StartRound();
        ret.roomID = roomID;
        ret.room_SetWait = room_SetWait;

        return ret;
    

    }
}