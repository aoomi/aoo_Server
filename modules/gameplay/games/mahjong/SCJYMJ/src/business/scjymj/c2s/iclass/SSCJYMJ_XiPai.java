package business.scjymj.c2s.iclass;

import cenum.ClassType;
import jsproto.c2s.cclass.BaseSendMsg;


public class SSCJYMJ_XiPai extends BaseSendMsg {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public long roomID;
    public long pid;
    public ClassType cType;

    public static SSCJYMJ_XiPai make(long roomID, long pid, ClassType cType) {
        SSCJYMJ_XiPai ret = new SSCJYMJ_XiPai();
        ret.roomID = roomID;
        ret.pid = pid;
        ret.cType = cType;
        return ret;


    }
}
