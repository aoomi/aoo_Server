package business.xcpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 接收客户端数据
 * 明牌
 * @author zaf
 *
 */

public class CXCPDK_OpenCard extends BaseSendMsg {

	public long roomID;
    public int pos;  //位置
    public int  OpenCard;//是否明牌  0:不明牌 1：明牌

    public static CXCPDK_OpenCard make(long roomID,int pos, int OpenCard) {
    	CXCPDK_OpenCard ret = new CXCPDK_OpenCard();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.OpenCard = OpenCard;
        return ret;
    }
}
