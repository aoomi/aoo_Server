package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 获取俱乐部游戏设置
 *
 * @author zaf
 */
public class CClub_Close extends BaseSendMsg {

    public long clubId;//俱乐部ID

    public static CClub_Close make(long clubId) {
        CClub_Close ret = new CClub_Close();
        ret.clubId = clubId;
        return ret;
    }
}