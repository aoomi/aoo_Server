package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class SSCJYMJ_SetEnd<T> extends BaseSendMsg {

    public long roomID;
    public T setEnd;


    public static <T> SSCJYMJ_SetEnd make(long roomID, T setEnd) {
        SSCJYMJ_SetEnd ret = new SSCJYMJ_SetEnd();
        ret.roomID = roomID;
        ret.setEnd = setEnd;

        return ret;


    }
}
