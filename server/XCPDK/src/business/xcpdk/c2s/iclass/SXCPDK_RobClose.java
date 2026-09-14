package business.xcpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 接收客户端数据
 * 抢关门
 * @author zaf
 *
 */

public class SXCPDK_RobClose extends BaseSendMsg {

	public long roomID;
    public int pos;  //位置
    public int  robClose;//是否抢关门  0:否 1：是

    public static SXCPDK_RobClose make(long roomID,int pos, int robClose) {
    	SXCPDK_RobClose ret = new SXCPDK_RobClose();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.robClose = robClose;
        return ret;
    }
}
