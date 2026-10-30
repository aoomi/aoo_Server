package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;

public class SPlayer_FindPlayBack extends BaseSendMsg {
    public int gameId;
    public int playBackCode;

    public static SPlayer_FindPlayBack make(int gameId, int playBackCode) {
        SPlayer_FindPlayBack ret = new SPlayer_FindPlayBack();
        ret.gameId = gameId;
        ret.playBackCode = playBackCode;
        return ret;
    }


}
