package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

@SuppressWarnings("serial")
public class CSCJYMJ_OpPiao extends BaseSendMsg {

    public long roomID;
    public int value;

    public static CSCJYMJ_OpPiao make(long roomID, int value) {
        CSCJYMJ_OpPiao ret = new CSCJYMJ_OpPiao();
        ret.roomID = roomID;
        ret.value = value;

        return ret;

    }
}
