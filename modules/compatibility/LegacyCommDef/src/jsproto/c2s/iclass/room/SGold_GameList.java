package jsproto.c2s.iclass.room;

import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.room.GoldItem;

import java.util.List;

/**
 * 练习场游戏列表
 *
 * @author Huaxing
 */
public class SGold_GameList extends BaseSendMsg {
    public List<GoldItem> gameLists;

    public static SGold_GameList make(List<GoldItem> gameLists) {
        SGold_GameList ret = new SGold_GameList();
        ret.gameLists = gameLists;
        return ret;
    }

}
