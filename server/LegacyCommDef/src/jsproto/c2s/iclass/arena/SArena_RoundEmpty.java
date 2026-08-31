package jsproto.c2s.iclass.arena;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 通知轮空的玩家
 *
 * @author Huaxing
 */
public class SArena_RoundEmpty extends BaseSendMsg {
    public String name;
    public long aid = 0L;

    public static SArena_RoundEmpty make(String name, long aid) {
        SArena_RoundEmpty ret = new SArena_RoundEmpty();
        ret.aid = aid;
        ret.name = name;
        return ret;
    }
}
