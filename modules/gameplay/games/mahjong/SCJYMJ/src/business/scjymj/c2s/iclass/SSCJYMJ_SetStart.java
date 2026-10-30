package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class SSCJYMJ_SetStart<T> extends BaseSendMsg {

    public long roomID;
    public T setInfo;


    public static <T> SSCJYMJ_SetStart make(long roomID, T setInfo) {
        SSCJYMJ_SetStart ret = new SSCJYMJ_SetStart();
        ret.roomID = roomID;
        ret.setInfo = setInfo;
        //打印数组看看

        return ret;
    }
}
