package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class SNJPDK_ChangePlayerNumAgree extends BaseSendMsg {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public long roomID;
    public int pos;
    public boolean agreeChange;

    public static SNJPDK_ChangePlayerNumAgree make(long roomID, int pos, boolean agreeChange) {
        SNJPDK_ChangePlayerNumAgree ret = new SNJPDK_ChangePlayerNumAgree();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.agreeChange = agreeChange;
        return ret;


    }
}
