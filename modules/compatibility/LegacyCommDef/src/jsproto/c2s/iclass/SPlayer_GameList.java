package jsproto.c2s.iclass;

import jsproto.c2s.cclass.BaseSendMsg;


/**
 * 游戏列表
 *
 * @author Huaxing
 */
public class SPlayer_GameList extends BaseSendMsg {
    public String gameList = "";

    public static SPlayer_GameList make(String gameList) {
        SPlayer_GameList ret = new SPlayer_GameList();
        ret.gameList = gameList;
        return ret;
    }

}
