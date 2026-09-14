package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class SSCJYMJ_StartRound<T> extends BaseSendMsg {

    public long roomID;
    public T room_SetWait;


    public static <T> SSCJYMJ_StartRound make(long roomID, T room_SetWait) {
        SSCJYMJ_StartRound ret = new SSCJYMJ_StartRound();
        ret.roomID = roomID;
        ret.room_SetWait = room_SetWait;

        return ret;


    }
}
