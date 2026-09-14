package jsproto.c2s.iclass.mj;

import cenum.mj.OpType;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 补花
 *
 * @param <T>
 * @author Huaxing
 */
@SuppressWarnings("serial")
public class _Applique<T> extends BaseSendMsg {
    public long roomID;
    public int pos;
    public OpType opType;
    public int opCard;
    public int normalMoCnt;
    public int gangMoCnt;
    public boolean isFlash;
    public T set_Pos;


    @SuppressWarnings("rawtypes")
    public static <T> _Applique make(long roomID, int pos, OpType opType, int opCard, boolean isFlash, T set_Pos, int normalMoCnt, int gangMoCnt, String gameNameStr) {
        _Applique ret = new _Applique();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.opType = opType;
        ret.opCard = opCard;
        ret.isFlash = isFlash;
        ret.set_Pos = set_Pos;
        ret.normalMoCnt = normalMoCnt;
        ret.gangMoCnt = gangMoCnt;
        ret.setGameNameStr(gameNameStr);
        return ret;


    }
}
