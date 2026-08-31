package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class SNJPDK_ChangeRoomNum extends BaseSendMsg {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    public long roomID;
    public String roomKey;
    public int createType;

    public static SNJPDK_ChangeRoomNum make(long roomID, String roomKey, int createType) {
        SNJPDK_ChangeRoomNum ret = new SNJPDK_ChangeRoomNum();
        ret.roomID = roomID;
        ret.roomKey = roomKey;
        ret.createType = createType;
        return ret;
    }
}
