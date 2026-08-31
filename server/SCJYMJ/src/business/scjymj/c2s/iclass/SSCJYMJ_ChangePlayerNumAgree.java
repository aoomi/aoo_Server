package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class SSCJYMJ_ChangePlayerNumAgree extends BaseSendMsg {

    private static final long serialVersionUID = 1L;
    public long roomID;
    public int pos;
    public boolean agreeChange;

    public static SSCJYMJ_ChangePlayerNumAgree make(long roomID, int pos, boolean agreeChange) {
        SSCJYMJ_ChangePlayerNumAgree ret = new SSCJYMJ_ChangePlayerNumAgree();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.agreeChange = agreeChange;
        return ret;


    }
}
