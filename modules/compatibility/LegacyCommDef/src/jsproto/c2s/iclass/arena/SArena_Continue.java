package jsproto.c2s.iclass.arena;

import jsproto.c2s.cclass.BaseSendMsg;

public class SArena_Continue extends BaseSendMsg {
    public int curRound;
    public int round;

    public static SArena_Continue make(int curRound, int round) {
        SArena_Continue ret = new SArena_Continue();
        ret.curRound = curRound;
        ret.round = round;
        return ret;
    }
}
