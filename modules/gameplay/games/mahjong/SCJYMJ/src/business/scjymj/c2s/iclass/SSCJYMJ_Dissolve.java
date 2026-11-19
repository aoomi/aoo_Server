package business.scjymj.c2s.iclass;

import jsproto.c2s.iclass.room.SBase_Dissolve;

/**
 * 房间解散通知
 *
 * @author Administrator
 */
public class SSCJYMJ_Dissolve extends SBase_Dissolve {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static SSCJYMJ_Dissolve make(SBase_Dissolve dissolve) {
        SSCJYMJ_Dissolve ret = new SSCJYMJ_Dissolve();
        ret.setRoomID(dissolve.getRoomID());
        ret.setOwnnerForce(dissolve.isOwnnerForce());
        ret.setDissolveNoticeType(dissolve.getDissolveNoticeType());
        ret.setMsg(dissolve.getMsg());
        return ret;
    }
}
