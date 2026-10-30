package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 获取俱乐部游戏设置
 *
 * @author zaf
 */
public class CClub_CreateGameSetChange extends BaseSendMsg {

    public long gameIndex;//俱乐部房间设置唯一值
    public long clubId;//俱乐部ID
    public int status;//俱乐部房间状态

    public static CClub_CreateGameSetChange make(long clubId, long gameIndex, int status) {
        CClub_CreateGameSetChange ret = new CClub_CreateGameSetChange();
        ret.gameIndex = gameIndex;
        ret.clubId = clubId;
        ret.status = status;
        return ret;
    }
}