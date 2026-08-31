package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 状态改变
 */
public class SNJPDK_ChangeStatus extends BaseSendMsg {

    public long roomID;
    public int state;  //位置
    public int opPos;//操作位

    public static SNJPDK_ChangeStatus make(long roomID, int state, int opPos) {
        SNJPDK_ChangeStatus ret = new SNJPDK_ChangeStatus();
        ret.roomID = roomID;
        ret.state = state;
        ret.opPos = opPos;
        return ret;
    }
}
