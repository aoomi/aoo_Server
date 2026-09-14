package jsproto.c2s.iclass.arena;

import cenum.ArenaEnum.EnrollEnum;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 比赛场通知
 *
 * @author Huaxing
 */
public class SArena_Notice extends BaseSendMsg {
    public EnrollEnum aTypeEnum;
    public long aid = 0;
    public int size = 0;

    public static SArena_Notice make(EnrollEnum aTypeEnum, long aid, int size) {
        SArena_Notice ret = new SArena_Notice();
        ret.aTypeEnum = aTypeEnum;
        ret.aid = aid;
        ret.size = size;
        return ret;
    }
}
