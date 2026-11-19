package jsproto.c2s.iclass.club;

import jsproto.c2s.cclass.BaseSendMsg;

/**
 * 获取俱乐部游戏设置
 *
 * @author zaf
 */
public class CClub_GetRank extends BaseSendMsg {

    public long clubId;//俱乐部ID
    /**
     * @see jsproto.c2s.cclass.club.Club_define.Club_CostRoomCard_Rank
     */
    public int type;//获取月度还是年度  0月度  1年度

    public int costCardType = 0;//0:金币,1:圈卡

    public static CClub_GetRank make(long clubId, int type) {
        CClub_GetRank ret = new CClub_GetRank();
        ret.clubId = clubId;
        ret.type = type;
        return ret;
    }
}