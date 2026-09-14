package jsproto.c2s.iclass.club;

import cenum.VisitSignEnum;
import jsproto.c2s.cclass.BaseSendMsg;
import jsproto.c2s.cclass.room.RoomInfoItemShortOne;

import java.util.List;

/**
 * 获取俱乐部房间信息
 *
 * @author zaf
 */
public class SClub_GetAllRoomMin extends BaseSendMsg {
    public long clubId;
    public long unionId;
    public List<RoomInfoItemShortOne> roomList;

    public static SClub_GetAllRoomMin make(long clubId, List<RoomInfoItemShortOne> roomList) {
        SClub_GetAllRoomMin ret = new SClub_GetAllRoomMin();
        ret.clubId = clubId;
        ret.unionId = 0L;
        ret.roomList = roomList;
        ret.setSignEnum(VisitSignEnum.CLUN_ROOM_MAIN);
        return ret;
    }
}