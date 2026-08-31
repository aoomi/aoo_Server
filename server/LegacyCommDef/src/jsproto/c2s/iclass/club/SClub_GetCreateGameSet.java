package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.club.ClubCreateGameSetInfo;

import java.util.List;

/**
 * 获取俱乐部游戏设置
 *
 * @author zaf
 */
public class SClub_GetCreateGameSet extends BaseSendMsg {

    public long clubId;//俱乐部ID
    public List<ClubCreateGameSetInfo> clubCreateGameSets;
    public int waitRoomCount;//等待中的房间
    public int playingRoomCount;//游戏中的房间
    /**
     * 是否自动开房
     *
     * @see jsproto.c2s.cclass.club.Club_define.Club_AUTOROOMCREATION
     */
    public int memberCreationRoom;

    public static SClub_GetCreateGameSet make(long clubId, int waitRoomCount, int playingRoomCount, List<ClubCreateGameSetInfo> clubCreateGameSets, int memberCreationRoom) {
        SClub_GetCreateGameSet ret = new SClub_GetCreateGameSet();
        ret.clubId = clubId;
        ret.clubCreateGameSets = clubCreateGameSets;
        ret.waitRoomCount = waitRoomCount;
        ret.playingRoomCount = playingRoomCount;
        ret.memberCreationRoom = memberCreationRoom;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}