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
public class CMJ_PiaoFen extends BaseSendMsg {

    public long roomID;
    public int piaoFen;

    public static CMJ_PiaoFen make(long roomID, int piaoFen) {
        CMJ_PiaoFen ret = new CMJ_PiaoFen();
        ret.roomID = roomID;
        ret.piaoFen = piaoFen;
        return ret;
    }
}
