package jsproto.c2s.iclass.arena;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 通知比赛场结束
 *
 * @author Huaxing
 */
public class SArena_End extends BaseSendMsg {
    public long aid = 0L;
    public String name;

    public static SArena_End make(long aid, String name) {
        SArena_End ret = new SArena_End();
        ret.aid = aid;
        ret.name = name;
        return ret;
    }
}
