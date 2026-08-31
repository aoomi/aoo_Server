package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class SSCJYMJ_Config<T> extends BaseSendMsg {
    public T cfg;


    public static <T> SSCJYMJ_Config make(T cfg) {
        SSCJYMJ_Config ret = new SSCJYMJ_Config();
        ret.cfg = cfg;
        return ret;


    }
}
