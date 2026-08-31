package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 跟庄消息
 * @author leo_wi
 */
public class _GenZhuang extends BaseSendMsg {
    public long roomID;
    public int dPos;
    public int count;//跟庄次数

    public static _GenZhuang make(long roomID, int dPos, int count, String gameNameStr) {
        _GenZhuang ret = new _GenZhuang();
        ret.roomID = roomID;
        ret.dPos = dPos;
        ret.count = count;
        ret.setGameNameStr(gameNameStr);
        return ret;

    }
}						
