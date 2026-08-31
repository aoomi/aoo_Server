package business.xcpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 接收客户端数据
 * 明牌
 * @author zaf
 *
 */

@SuppressWarnings("serial")
public class SXCPDK_ChangeStatus extends BaseSendMsg {

	public long roomID;
    public int state;  //位置
    public int opPos;//操作位
    public boolean isRobCloseSuccess;//是否是抢关门成功

    public static SXCPDK_ChangeStatus make(long roomID,int state, int opPos, boolean isRobCloseSuccess) {
    	SXCPDK_ChangeStatus ret = new SXCPDK_ChangeStatus();
        ret.roomID = roomID;
        ret.state = state;
        ret.opPos = opPos;
        ret.isRobCloseSuccess = isRobCloseSuccess;
        return ret;
    }
}
