package jsproto.c2s.iclass.mj;

import cenum.mj.OpType;
import jsproto.c2s.cclass.BaseSendMsg;


@SuppressWarnings("serial")
public class _PosOpCard<T> extends BaseSendMsg {

    public long roomID;
    public int pos;
    public T set_Pos;
    public OpType opType;
    public int opCard;
    public boolean isFlash;


    public static <T> _PosOpCard<T> make(long roomID, int pos, T set_Pos, OpType opType, int opCard, boolean isFlash, String gameNameStr) {
        _PosOpCard<T> ret = new _PosOpCard<T>();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.set_Pos = set_Pos;
        ret.opType = opType;
        ret.opCard = opCard;
        ret.isFlash = isFlash;
        ret.setGameNameStr(gameNameStr);
        return ret;


    }
}	
