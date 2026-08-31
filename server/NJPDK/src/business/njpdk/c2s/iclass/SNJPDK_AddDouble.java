package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 接收客户端数据
 * 加倍
 *
 * @author zaf
 */

@SuppressWarnings("serial")
public class SNJPDK_AddDouble extends BaseSendMsg {

    public long roomID;
    public int pos;  //位置
    public int addDouble;


    public static SNJPDK_AddDouble make(long roomID, int pos, int addDouble) {
        SNJPDK_AddDouble ret = new SNJPDK_AddDouble();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.addDouble = addDouble;
        return ret;
    }
}
