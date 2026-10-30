package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 买子通知操作
 *
 * @author Administrator
 */
@SuppressWarnings("serial")
public class SSCJYMJ_OpPiao extends BaseSendMsg {

    public long roomID;
    public int opPos;
    public int value;

    public static SSCJYMJ_OpPiao make(long roomID, int opPos, int value) {
        SSCJYMJ_OpPiao ret = new SSCJYMJ_OpPiao();
        ret.roomID = roomID;
        ret.opPos = opPos;
        ret.value = value;

        return ret;

    }
}
