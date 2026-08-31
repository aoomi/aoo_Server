package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 用户兑换
 *
 * @author Huaxing
 */
public class CPlayer_Exchange extends BaseSendMsg {
    public int productID;

    public static CPlayer_Exchange make(int productID) {
        CPlayer_Exchange ret = new CPlayer_Exchange();
        ret.productID = productID;
        return ret;


    }
}
