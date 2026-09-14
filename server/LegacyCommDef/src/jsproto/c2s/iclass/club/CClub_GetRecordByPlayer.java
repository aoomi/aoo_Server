package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 获取俱乐部游戏设置
 *
 * @author zaf
 */
public class CClub_GetRecordByPlayer extends BaseSendMsg {

    public long clubId;//俱乐部ID
    public long unionId;
    /**
     * @see jsproto.c2s.cclass.club.Club_define.Club_Record_Get_Type
     */
    public int getType;//获取时间 0今天,1昨天,2最近三天
    /**
     * 玩家pid
     */
    public long pid;

    public int pageNum;


    public static CClub_GetRecordByPlayer make(long clubId, int getType, long pid, int pageNum) {
        CClub_GetRecordByPlayer ret = new CClub_GetRecordByPlayer();
        ret.clubId = clubId;
        ret.getType = getType;
        ret.pid = pid;
        ret.pageNum = pageNum;
        return ret;
    }
}