package jsproto.c2s.iclass.arena;

import cenum.ArenaEnum.ArenaTimeEnum;
import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 比赛场-时间通知
 *
 * @author Huaxing
 */
public class SArena_TimeNotice extends BaseSendMsg {
    public String name;
    public ArenaTimeEnum aTimeEnum;
    public int minTime = -1;

    public static SArena_TimeNotice make(String name, ArenaTimeEnum aTimeEnum, int minTime) {
        SArena_TimeNotice ret = new SArena_TimeNotice();
        ret.name = name;
        ret.aTimeEnum = aTimeEnum;
        ret.minTime = minTime;
        return ret;
    }
}
