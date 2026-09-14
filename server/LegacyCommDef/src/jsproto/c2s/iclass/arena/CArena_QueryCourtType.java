package jsproto.c2s.iclass.arena;

import jsproto.c2s.cclass.BaseSendMsg;

public class CArena_QueryCourtType extends BaseSendMsg {
    public int courtType;
    public int gameType;
    public int clubId;

    public static CArena_QueryCourtType make(int courtType, int gameType, int clubId) {
        CArena_QueryCourtType ret = new CArena_QueryCourtType();
        ret.courtType = courtType;
        ret.gameType = gameType;
        ret.clubId = clubId;
        return ret;
    }
}
