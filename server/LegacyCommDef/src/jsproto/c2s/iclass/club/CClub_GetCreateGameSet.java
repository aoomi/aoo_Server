package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 获取俱乐部游戏设置
 *
 * @author zaf
 */
public class CClub_GetCreateGameSet extends BaseSendMsg {

    public long clubId;//俱乐部ID

    public static CClub_GetCreateGameSet make(long clubId) {
        CClub_GetCreateGameSet ret = new CClub_GetCreateGameSet();
        ret.clubId = clubId;
        return ret;
    }
}