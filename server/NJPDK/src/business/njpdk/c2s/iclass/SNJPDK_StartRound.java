package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


@SuppressWarnings("serial")
public class SNJPDK_StartRound<T> extends BaseSendMsg {

    public long roomID;
    public T room_SetWait;


    public static <T> SNJPDK_StartRound<T> make(long roomID, T room_SetWait) {
        SNJPDK_StartRound<T> ret = new SNJPDK_StartRound<T>();
        ret.roomID = roomID;
        ret.room_SetWait = room_SetWait;

        return ret;


    }
}
