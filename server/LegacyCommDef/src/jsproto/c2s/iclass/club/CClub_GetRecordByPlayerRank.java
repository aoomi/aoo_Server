package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 获取俱乐部游戏设置
 *
 * @author zaf
 */
public class CClub_GetRecordByPlayerRank extends BaseSendMsg {

    public long clubId;//俱乐部ID
    /**
     * @see jsproto.c2s.cclass.club.Club_define.Club_Record_Get_Type
     */
    public int getType;//获取时间 0今天,1昨天,2最近三天
    public int pageNum;

    public static CClub_GetRecordByPlayerRank make(long clubId, int getType, int pageNum) {
        CClub_GetRecordByPlayerRank ret = new CClub_GetRecordByPlayerRank();
        ret.clubId = clubId;
        ret.getType = getType;
        ret.pageNum = pageNum;
        return ret;
    }
}