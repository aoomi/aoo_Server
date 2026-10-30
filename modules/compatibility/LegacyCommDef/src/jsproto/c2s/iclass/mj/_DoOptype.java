package jsproto.c2s.iclass.mj;

import cenum.mj.OpType;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 莆田麻将 客户点过消息
 *
 * @author leowi
 */
@SuppressWarnings("serial")
public class _DoOptype extends BaseSendMsg {
    public long roomID;
    public OpType opType;
    public int pos;
    public long doTime;

    public static _DoOptype make(long roomID, int pos, OpType opType, long doTime, String gameNameStr) {
        _DoOptype ret =new _DoOptype();
        ret.roomID = roomID;
        ret.pos = pos;
        ret.doTime = doTime;
        ret.opType = opType;
        ret. setGameNameStr(gameNameStr);
        return ret;
    }


}