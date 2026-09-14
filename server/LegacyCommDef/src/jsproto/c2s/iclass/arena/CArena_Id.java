package jsproto.c2s.iclass.arena;

import jsproto.c2s.cclass.BaseSendMsg;

public class CArena_Id extends BaseSendMsg {
    public long aid = 0L;

    public static CArena_Id make(long aid) {
        CArena_Id ret = new CArena_Id();
        ret.aid = aid;
        return ret;
    }
}
