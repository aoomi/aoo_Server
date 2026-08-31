package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.club.ClubCreateGameSetInfo;

/**
 * 获取俱乐部游戏设置
 *
 * @author zaf
 */
public class SClub_CreateGameSetChange extends BaseSendMsg {
    public long clubId;//俱乐部ID
    public ClubCreateGameSetInfo clubCreateGameSet;//俱乐部房间状态
    public boolean isCreate;//是否是创建
    public long pid;//谁操作
    public int waitRoomCount;//等待中的房间
    public int playingRoomCount;//游戏中的房间
    /**
     * 是否自动开房
     *
     * @see jsproto.c2s.cclass.club.Club_define.Club_AUTOROOMCREATION
     */
    public int memberCreationRoom;

    public static SClub_CreateGameSetChange make(long clubId, long pid, boolean isCreate, ClubCreateGameSetInfo clubCreateGameSet
            , int waitRoomCount, int playingRoomCount, int memberCreationRoom) {
        SClub_CreateGameSetChange ret = new SClub_CreateGameSetChange();
        ret.clubId = clubId;
        ret.clubCreateGameSet = clubCreateGameSet;
        ret.isCreate = isCreate;
        ret.pid = pid;
        ret.waitRoomCount = waitRoomCount;
        ret.playingRoomCount = playingRoomCount;
        ret.memberCreationRoom = memberCreationRoom;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}