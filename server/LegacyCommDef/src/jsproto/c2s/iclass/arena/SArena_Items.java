package jsproto.c2s.iclass.arena;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.arena.ArenaItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 比赛场列表项
 *
 * @author Huaxing
 */
public class SArena_Items extends BaseSendMsg {
    public List<ArenaItem> aItems = new ArrayList<ArenaItem>();

    public static SArena_Items make(List<ArenaItem> aItems) {
        SArena_Items ret = new SArena_Items();
        ret.aItems = aItems;
        return ret;
    }
}
