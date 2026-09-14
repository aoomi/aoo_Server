package business.xcpdk.c2s.iclass;

import java.util.List;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 接收客户端数据
 * 明牌
 * @author zaf
 *
 */

public class SXCPDK_OpenCard extends BaseSendMsg {

	public long roomID;
    public int pos;  //位置
    public int  OpenCard;//是否明牌  0:不明牌 1：明牌
    public List<Integer> cardList;

    public static SXCPDK_OpenCard make(long roomID,int pos, int  OpenCard, List<Integer> cardList) {
    	SXCPDK_OpenCard ret = new SXCPDK_OpenCard();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.OpenCard = OpenCard;
        ret.cardList = cardList;
        return ret;
    }
}
