package business.xcpdk.c2s.iclass;
import business.xcpdk.c2s.cclass.XCPDKRoomSetInfo;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 一局游戏开始
 * @author zaf
 * */
@SuppressWarnings("serial")
public class SXCPDK_SetStart extends BaseSendMsg {

    public long roomID;
    public XCPDKRoomSetInfo setInfo;

    public static SXCPDK_SetStart make(long roomID, XCPDKRoomSetInfo setInfo) {
        SXCPDK_SetStart ret = new SXCPDK_SetStart();
        ret.roomID = roomID;
        ret.setInfo = setInfo;
        return ret;
    }
}
