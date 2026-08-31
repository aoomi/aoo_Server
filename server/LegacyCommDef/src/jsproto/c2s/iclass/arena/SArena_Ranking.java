package jsproto.c2s.iclass.arena;

import jsproto.c2s.cclass.BaseSendMsg;

public class SArena_Ranking extends BaseSendMsg {
    public int type; // 1：打立出局,2：瑞士移位
    public int current; // 当前分数
    public int limit; // 界限
    public int state = 0;
    public int proState = 0;// 是否淘汰

    public static SArena_Ranking make(int type, int current, int limit, int state, int proState) {
        SArena_Ranking ret = new SArena_Ranking();
        ret.type = type;
        ret.current = current;
        ret.limit = limit;
        ret.state = state;
        ret.proState = proState;
        return ret;

    }
}