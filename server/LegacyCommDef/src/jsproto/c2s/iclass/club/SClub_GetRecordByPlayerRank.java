package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.club.ClubRecordByPlayerRank;

import java.util.List;

/**
 * 获取俱乐部游戏设置
 *
 * @author zaf
 */
public class SClub_GetRecordByPlayerRank extends BaseSendMsg {

    public long clubId;//俱乐部ID
    /**
     * @see jsproto.c2s.cclass.club.Club_define.Club_Record_Get_Type
     */
    public int getType;//获取时间 0今天,1昨天,2最近三天

    /**
     * 俱乐部玩家排行
     */
    public List<ClubRecordByPlayerRank> clubPlayerRecords;
    /**
     * 开房总次数
     */
    public int roomTotalCount;
    /**
     * 开房总消耗
     */
    public int roomCardTotalCount;

    public int clubCardTotalCount;

    public static SClub_GetRecordByPlayerRank make(long clubId, int getType, int roomCardTotalCount, int roomTotalCount, int clubCardTotalCount, List<ClubRecordByPlayerRank> clubPlayerRecords) {
        SClub_GetRecordByPlayerRank ret = new SClub_GetRecordByPlayerRank();
        ret.clubId = clubId;
        ret.getType = getType;
        ret.clubPlayerRecords = clubPlayerRecords;
        ret.roomCardTotalCount = roomCardTotalCount;
        ret.roomTotalCount = roomTotalCount;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}