package jsproto.c2s.iclass.pk;

import jsproto.c2s.cclass.BaseSendMsg;

import java.util.List;

/**
 * 出牌列表
 */
public class SPDK_OutCardList extends BaseSendMsg {

    public int pos;
    public int opCardType;
    public List<Integer> cardList;

    public static SPDK_OutCardList make(int pos, int opCardType, List<Integer> cardList) {
        SPDK_OutCardList ret = new SPDK_OutCardList();
        ret.pos = pos;
        ret.opCardType = opCardType;
        ret.cardList = cardList;
        return ret;
    }
}
