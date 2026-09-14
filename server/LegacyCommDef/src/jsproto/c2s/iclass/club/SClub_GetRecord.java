package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.club.ClubRecordInfo;

import java.util.List;

/**
 * 获取俱乐部游戏设置
 *
 * @author zaf
 */
public class SClub_GetRecord extends BaseSendMsg {

    public long clubId;//俱乐部ID

    public long unionId;
    /**
     * @see jsproto.c2s.cclass.club.Club_define.Club_Record_Get_Type
     */
    public int getType;//获取时间 0今天,1昨天,2最近三天

    /**
     * 俱乐部玩家排行
     */
    public List<ClubRecordInfo> clubRecordInfos;


    public static SClub_GetRecord make(long clubId, long unionId, int getType, List<ClubRecordInfo> clubRecordInfos) {
        SClub_GetRecord ret = new SClub_GetRecord();
        ret.clubId = clubId;
        ret.unionId = unionId;
        ret.getType = getType;
        ret.clubRecordInfos = clubRecordInfos;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}