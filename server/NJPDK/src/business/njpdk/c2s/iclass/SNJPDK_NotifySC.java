package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 报单通知
 */
public class SNJPDK_NotifySC extends BaseSendMsg {
    public long roomID;
    public int pos;  //位置

    public static SNJPDK_NotifySC make(long roomID, int pos) {
        SNJPDK_NotifySC sc = new SNJPDK_NotifySC();
        sc.roomID = roomID;
        sc.pos = pos;
        return sc;
    }
}
