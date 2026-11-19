package jsproto.c2s.iclass.rank;

import jsproto.c2s.cclass.BaseSendMsg;

public class CRanking_List extends BaseSendMsg {
    public int rankQueryType = 0;

    public static CRanking_List make(int rankQueryType) {
        CRanking_List ret = new CRanking_List();
        ret.rankQueryType = rankQueryType;
        return ret;


    }
}
