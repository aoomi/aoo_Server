package business.njpdk.c2s.iclass;

import business.njpdk.c2s.cclass.NJPDKRoomSetInfo;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 安岳一局游戏开始
 *
 * @author zaf
 */
public class SNJPDK_SetStart extends BaseSendMsg {

    public long roomID;
    public NJPDKRoomSetInfo setInfo;

    public static SNJPDK_SetStart make(long roomID, NJPDKRoomSetInfo setInfo) {
        SNJPDK_SetStart ret = new SNJPDK_SetStart();
        ret.roomID = roomID;
        ret.setInfo = setInfo;
        return ret;
    }
}
