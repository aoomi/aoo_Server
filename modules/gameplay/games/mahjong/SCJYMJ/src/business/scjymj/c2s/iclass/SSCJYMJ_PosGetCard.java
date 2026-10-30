package business.scjymj.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


public class SSCJYMJ_PosGetCard<T> extends BaseSendMsg {

    public long roomID;
    public int pos;
    public int normalMoCnt;
    public int gangMoCnt;
    public T set_Pos;
    public int cardRestSize;

    public static <T> SSCJYMJ_PosGetCard make(long roomID, int pos, int normalMoCnt, int gangMoCnt, T set_Pos, int cardRestSize) {
        SSCJYMJ_PosGetCard ret = new SSCJYMJ_PosGetCard();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.normalMoCnt = normalMoCnt;
        ret.gangMoCnt = gangMoCnt;
        ret.set_Pos = set_Pos;
        ret.cardRestSize = cardRestSize;
        return ret;


    }
}
