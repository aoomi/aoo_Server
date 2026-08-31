package business.njpdk.c2s.iclass;

import cenum.ClassType;
import jsproto.c2s.cclass.BaseSendMsg;


public class SNJPDK_XiPai extends BaseSendMsg {
    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public long roomID;
    public long pid;
    public ClassType cType;

    public static SNJPDK_XiPai make(long roomID, long pid, ClassType cType) {
        SNJPDK_XiPai ret = new SNJPDK_XiPai();
        ret.roomID = roomID;
        ret.pid = pid;
        ret.cType = cType;
        return ret;


    }
}
