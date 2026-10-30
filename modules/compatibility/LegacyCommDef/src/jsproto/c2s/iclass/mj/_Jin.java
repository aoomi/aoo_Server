package jsproto.c2s.iclass.mj;

import jsproto.c2s.cclass.BaseSendMsg;

@SuppressWarnings("serial")
public class _Jin extends BaseSendMsg {
    public long roomID;
    public int benJin;
    public int jin1;
    public int jin2;
    public int jinJin ;
    public int normalMoCnt = 0; // 普通摸牌数量
    public int gangMoCnt = 0; // 杠后摸牌数量

    public static _Jin make(long roomID, int jin, int jin2,int jinJin,int benJin, int normalMoCnt, int gangMoCnt,String gameNameStr) {
        _Jin ret = new _Jin();
        ret.roomID = roomID;
        ret.jin1 = jin;
        ret.jin2 = jin2;
        ret.jinJin = jinJin;
        ret.benJin = benJin;
        ret.normalMoCnt = normalMoCnt;
        ret.gangMoCnt = gangMoCnt;
        ret.setGameNameStr(gameNameStr);
        return ret;

    }
}
