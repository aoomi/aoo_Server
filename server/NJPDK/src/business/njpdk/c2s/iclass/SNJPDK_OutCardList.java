package business.njpdk.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;

/**
 * 出牌列表
 */
public class SNJPDK_OutCardList extends BaseSendMsg {

    public int pos;
    public int opCardType;
    public List<Integer> cardList;

    public static SNJPDK_OutCardList make(int pos, int opCardType, List<Integer> cardList) {
        SNJPDK_OutCardList ret = new SNJPDK_OutCardList();
        ret.pos = pos;
        ret.opCardType = opCardType;
        ret.cardList = cardList;
        return ret;
    }
}
