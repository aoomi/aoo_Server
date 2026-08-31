package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;

/**
 * 接收客户端数据
 * 明牌
 *
 * @author zaf
 */

public class SNJPDK_OpenCard extends BaseSendMsg {

    public long roomID;
    public int pos;  //位置
    public int OpenCard;//是否明牌  0:不明牌 1：明牌
    public List<Integer> cardList;

    public static SNJPDK_OpenCard make(long roomID, int pos, int OpenCard, List<Integer> cardList) {
        SNJPDK_OpenCard ret = new SNJPDK_OpenCard();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.OpenCard = OpenCard;
        ret.cardList = cardList;
        return ret;
    }
}
