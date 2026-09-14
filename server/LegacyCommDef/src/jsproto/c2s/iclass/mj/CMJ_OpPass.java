package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 莆田麻将
 * 接收客户端数据
 * 创建房间
 *
 * @author Huaxing
 */
@SuppressWarnings("serial")
public class CMJ_OpPass extends BaseSendMsg {

    public long roomID;//房间ID
    public int opType = 0;

    public static CMJ_OpPass make(long roomID, int opType) {
        CMJ_OpPass ret = new CMJ_OpPass();
        ret.roomID = roomID;
        ret.opType = opType;
        return ret;
    }
}
