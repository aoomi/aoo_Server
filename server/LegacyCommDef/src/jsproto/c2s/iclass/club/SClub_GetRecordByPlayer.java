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
public class SClub_GetRecordByPlayer extends BaseSendMsg {

    public long clubId;//俱乐部ID
    /**
     * @see jsproto.c2s.cclass.club.Club_define.Club_Record_Get_Type
     */
    public int getType;//获取时间 0今天,1昨天,2最近三天
    /**
     * 玩家pid
     */
    public long pid;
    /**
     * 俱乐部玩家排行
     */
    public List<ClubRecordInfo> clubRecordInfos;


    public static SClub_GetRecordByPlayer make(long clubId, int getType, long pid, List<ClubRecordInfo> clubRecordInfos) {
        SClub_GetRecordByPlayer ret = new SClub_GetRecordByPlayer();
        ret.clubId = clubId;
        ret.getType = getType;
        ret.pid = pid;
        ret.clubRecordInfos = clubRecordInfos;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}