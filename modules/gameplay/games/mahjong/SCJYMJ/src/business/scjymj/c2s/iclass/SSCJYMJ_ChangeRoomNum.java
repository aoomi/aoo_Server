package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class SSCJYMJ_ChangeRoomNum extends BaseSendMsg {

    private static final long serialVersionUID = 1L;
    public long roomID;
    public String roomKey;
    public int createType;

    public static SSCJYMJ_ChangeRoomNum make(long roomID, String roomKey, int createType) {
        SSCJYMJ_ChangeRoomNum ret = new SSCJYMJ_ChangeRoomNum();
        ret.roomID = roomID;
        ret.roomKey = roomKey;
        ret.createType = createType;
        return ret;
    }
}
