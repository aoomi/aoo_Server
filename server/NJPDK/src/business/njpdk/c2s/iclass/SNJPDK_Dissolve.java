package business.njpdk.c2s.iclass;

import jsproto.c2s.iclass.room.SBase_Dissolve;

/**
 * 房间解散通知
 *
 * @author Administrator
 */
public class SNJPDK_Dissolve extends SBase_Dissolve {


    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public static SNJPDK_Dissolve make(SBase_Dissolve dissolve) {
        SNJPDK_Dissolve ret = new SNJPDK_Dissolve();
        ret.setOwnnerForce(dissolve.isOwnnerForce());
        ret.setRoomID(dissolve.getRoomID());
        ret.setDissolveNoticeType(dissolve.getDissolveNoticeType());
        ret.setMsg(dissolve.getMsg());
        return ret;
    }
}
